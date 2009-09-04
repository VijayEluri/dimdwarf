// Copyright © 2008-2009, Esko Luontola. All Rights Reserved.
// This software is released under the MIT License.
// The license may be viewed at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.gc.cms;

import com.google.inject.*;
import jdave.*;
import jdave.junit4.JDaveRunner;
import net.orfjackal.dimdwarf.api.EntityInfo;
import net.orfjackal.dimdwarf.api.internal.ObjectIdMigration;
import net.orfjackal.dimdwarf.entities.*;
import net.orfjackal.dimdwarf.gc.entities.GarbageCollectorManager;
import net.orfjackal.dimdwarf.modules.CommonModules;
import net.orfjackal.dimdwarf.modules.options.CmsGarbageCollectionOption;
import net.orfjackal.dimdwarf.server.TestServer;
import net.orfjackal.dimdwarf.tasks.*;
import net.orfjackal.dimdwarf.util.Objects;
import org.junit.runner.RunWith;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.*;
import java.util.logging.Level;

/**
 * @author Esko Luontola
 * @since 10.12.2008
 */
@RunWith(JDaveRunner.class)
@Group({"fast"})
public class ConcurrentMarkSweepCollectorIntegrationSpec extends Specification<Object> {

    private Provider<EntityInfo> info;
    private Provider<EntityRepository> entities;
    private Provider<BindingRepository> bindings;
    private Executor taskContext;
    private TestServer server;

    private GarbageCollectorManager gc;

    private ObjectIdMigration liveRootId;
    private ObjectIdMigration liveRefId;
    private ObjectIdMigration garbageRootId;
    private ObjectIdMigration garbageRefId;
    private ObjectIdMigration garbageCycleId1;
    private ObjectIdMigration garbageCycleId2;

    public void create() throws Exception {
        server = new TestServer(
                new CommonModules(),
                new CmsGarbageCollectionOption()
        );
        server.hideStartupShutdownLogs();
        server.start();

        Injector injector = server.getInjector();
        info = injector.getProvider(EntityInfo.class);
        entities = injector.getProvider(EntityRepository.class);
        bindings = injector.getProvider(BindingRepository.class);
        taskContext = injector.getInstance(TaskExecutor.class);

        gc = injector.getInstance(GarbageCollectorManager.class);

        initGraphNoGarbage();
        gc.runGarbageCollector(); // reset the node colors to white
        createGarbage();
    }

    public void destroy() throws Exception {
        server.shutdownIfRunning();
    }

    private void initGraphNoGarbage() {
        taskContext.execute(new Runnable() {
            public void run() {
                DummyEntity liveRoot = new DummyEntity();
                liveRootId = info.get().getEntityId(liveRoot);
                bindings.get().update("live", liveRoot);

                DummyEntity liveRef = new DummyEntity();
                liveRefId = info.get().getEntityId(liveRef);
                List<DummyInterface> list = new ArrayList<DummyInterface>();
                list.add(liveRef);
                liveRoot.setOther(list);

                DummyEntity garbageRoot = new DummyEntity();
                garbageRootId = info.get().getEntityId(garbageRoot);
                bindings.get().update("garbage", garbageRoot);

                DummyEntity garbageRef = new DummyEntity();
                garbageRefId = info.get().getEntityId(garbageRef);
                garbageRoot.setOther(garbageRef);

                DummyEntity garbageCycle1 = new DummyEntity();
                DummyEntity garbageCycle2 = new DummyEntity();
                garbageCycleId1 = info.get().getEntityId(garbageCycle1);
                garbageCycleId2 = info.get().getEntityId(garbageCycle1);
                garbageCycle1.setOther(garbageCycle2);
                garbageCycle2.setOther(garbageCycle1);
                garbageRef.setOther(garbageCycle1);
            }
        });
    }

    private void createGarbage() {
        taskContext.execute(new Runnable() {
            public void run() {
                bindings.get().delete("garbage");
            }
        });
    }

    private boolean entityExists(final ObjectIdMigration id) {
        final AtomicBoolean exists = new AtomicBoolean(false);
        taskContext.execute(new Runnable() {
            public void run() {
                exists.set(entities.get().exists(id));
            }
        });
        return exists.get();
    }


    public class WhenGarbageCollectorIsRun {

        public void create() {
            gc.runGarbageCollector();
        }

        public void liveNodesAreKept() {
            specify(entityExists(liveRootId));
            specify(entityExists(liveRefId));
        }

        public void garbageRootNodesAreCollected() {
            specify(entityExists(garbageRootId), should.equal(false));
        }

        public void garbageNodesAreCollected() {
            specify(entityExists(garbageRefId), should.equal(false));
        }

        public void garbageCyclesAreCollected() {
            specify(entityExists(garbageCycleId1), should.equal(false));
            specify(entityExists(garbageCycleId2), should.equal(false));
        }
    }

    public class WhenThereAreMutationsDuringGarbageCollection {

        private List<ObjectIdMigration> liveNodesCreated = new ArrayList<ObjectIdMigration>();

        public void create() throws Throwable {
            server.changeLoggingLevel(TransactionFilter.class, Level.WARNING);
            server.changeLoggingLevel(RetryingTaskExecutor.class, Level.WARNING);

            final AtomicReference<Throwable> failureInGcThread = new AtomicReference<Throwable>();
            Thread gcThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        gc.runGarbageCollector();
                    } catch (Throwable t) {
                        failureInGcThread.set(t);
                    }
                }
            });

            gcThread.start();
            for (int i = 0; i < 10; i++) {
                taskContext.execute(new Runnable() {
                    public void run() {
                        ObjectIdMigration id = createALiveNode();
                        liveNodesCreated.add(id);
                    }
                });
                Thread.yield();
            }
            gcThread.join();

            Throwable t = failureInGcThread.get();
            if (t != null) {
                throw t;
            }
        }

        private ObjectIdMigration createALiveNode() {
            DummyInterface liveRoot = (DummyInterface) entities.get().read(liveRootId);
            List<DummyInterface> childrenOfRoot = Objects.uncheckedCast(liveRoot.getOther());
            DummyEntity liveNode = new DummyEntity();
            childrenOfRoot.add(liveNode);
            return info.get().getEntityId(liveNode);
        }

        public void theMutatorListenerMakesSureThatNoLiveNodesAreCollected() {
            for (ObjectIdMigration node : liveNodesCreated) {
                specify(node + " of " + liveNodesCreated, entityExists(node));
            }
        }
    }
}
