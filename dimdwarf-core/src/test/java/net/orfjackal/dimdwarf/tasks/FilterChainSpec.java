// Copyright © 2008-2009, Esko Luontola. All Rights Reserved.
// This software is released under the MIT License.
// The license may be viewed at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.tasks;

import jdave.*;
import jdave.junit4.JDaveRunner;
import org.junit.runner.RunWith;

import java.util.*;

/**
 * @author Esko Luontola
 * @since 12.11.2008
 */
@RunWith(JDaveRunner.class)
@Group({"fast"})
public class FilterChainSpec extends Specification<Object> {

    private FilterChain chain;
    private Runnable command = new DummyCommand();
    private List<String> executionOrder = new ArrayList<String>();

    public class WhenThereAreNoFilters {

        public void create() {
            chain = new FilterChain(new Filter[0]);
            chain.execute(command);
        }

        public void theCommandIsExecuted() {
            specify(executionOrder, should.containInOrder("command"));
        }
    }

    public class WhenThereIsOneFilter {

        public void create() {
            chain = new FilterChain(new Filter[]{
                    new DummyFilter("A")
            });
            chain.execute(command);
        }

        public void theFilterIsExecutedAroundTheCommand() {
            specify(executionOrder, should.containInOrder(
                    "enter A",
                    "command",
                    "exit A"
            ));
        }
    }

    public class WhenThereAreManyFilters {

        public void create() {
            chain = new FilterChain(new Filter[]{
                    new DummyFilter("A"),
                    new DummyFilter("B")
            });
            chain.execute(command);
        }

        public void theFiltersAreNestedAroundTheCommandInTheDeclaredOrder() {
            specify(executionOrder, should.containInOrder(
                    "enter A",
                    "enter B",
                    "command",
                    "exit B",
                    "exit A"
            ));
        }
    }


    private class DummyCommand implements Runnable {
        public void run() {
            executionOrder.add("command");
        }
    }

    private class DummyFilter implements Filter {
        private final String name;

        public DummyFilter(String name) {
            this.name = name;
        }

        public void filter(Runnable nextInChain) {
            executionOrder.add("enter " + name);
            nextInChain.run();
            executionOrder.add("exit " + name);
        }
    }
}
