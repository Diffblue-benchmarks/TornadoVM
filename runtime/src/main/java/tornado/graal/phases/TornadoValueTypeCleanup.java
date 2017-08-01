/*
 * Copyright 2012 James Clarkson.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tornado.graal.phases;

import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.graph.iterators.NodePredicate;
import org.graalvm.compiler.nodes.StructuredGraph;
import org.graalvm.compiler.nodes.java.NewInstanceNode;
import org.graalvm.compiler.nodes.util.GraphUtil;
import org.graalvm.compiler.phases.BasePhase;

public class TornadoValueTypeCleanup extends BasePhase<TornadoHighTierContext> {

    private static final NodePredicate valueTypeFilter = new NodePredicate() {

        @Override
        public boolean apply(Node node) {
            return node.hasNoUsages();
        }

    };

    @Override
    protected void run(StructuredGraph graph, TornadoHighTierContext context) {

        graph.getNodes().filter(NewInstanceNode.class).filter(valueTypeFilter)
                .forEach(instance -> {
                    GraphUtil.tryKillUnused(instance);
                });

    }

}
