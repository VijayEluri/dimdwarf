// Copyright © 2008-2010 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.modules;

import com.google.inject.*;
import net.orfjackal.dimdwarf.api.*;
import net.orfjackal.dimdwarf.entities.BindingRepository;
import net.orfjackal.dimdwarf.scheduler.*;
import net.orfjackal.dimdwarf.util.*;

import java.util.concurrent.*;

public class TaskSchedulingModule extends AbstractModule {

    protected void configure() {
        bind(TaskScheduler.class).to(TaskSchedulerImpl.class);
        bind(TaskProducer.class).to(TaskSchedulerImpl.class);

        bind(RecoverableSetFactory.class).to(RecoverableSetFactoryImpl.class);
        bind(Clock.class).to(SystemClock.class);
    }

    @Provides
    ExecutorService executorService() {
        return Executors.newCachedThreadPool();
    }

    private static class RecoverableSetFactoryImpl implements RecoverableSetFactory {
        @Inject public Provider<BindingRepository> bindings;
        @Inject public Provider<EntityInfo> info;

        public <T> RecoverableSet<T> create(String prefix) {
            return new RecoverableSetImpl<T>(prefix, bindings, info);
        }
    }
}
