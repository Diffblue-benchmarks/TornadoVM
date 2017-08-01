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
package tornado.runtime.graph.nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import tornado.common.TornadoDevice.CacheMode;
import tornado.runtime.graph.GraphAssembler;
import tornado.runtime.graph.GraphCompilationResult;

import static tornado.runtime.graph.GraphAssembler.encodeBlockingMode;
import static tornado.runtime.graph.GraphAssembler.encodeCacheMode;

public class WriteHostNode extends FixedDeviceOpNode {

    public WriteHostNode(TaskNode task) {
        super(task.getDevice());
        this.task = task;
    }

    private TaskNode task;
    private ParameterNode value;

    public void setValue(ParameterNode object) {
        value = object;
    }

    public ParameterNode getValue() {
        return value;
    }

    public TaskNode getTask() {
        return task;
    }

    @Override
    public String toString() {
        return String.format("[%d]: copy out parameter %d after task %d", id, value.getIndex(), task.getId());
    }

    @Override
    public boolean hasInputs() {
        return value != null;
    }

    @Override
    public List<AbstractNode> getInputs() {
        if (!hasInputs()) {
            return Collections.emptyList();
        }

        final List<AbstractNode> result = new ArrayList<>();
        result.add(value);
        return result;
    }

    @Override
    public void emit(GraphCompilationResult compRes) {
        final GraphAssembler asm = compRes.getAssembler();
        byte mode = encodeBlockingMode(getBlocking(), (byte) 0);
        mode = encodeCacheMode(CacheMode.CACHABLE, mode);
        asm.writeHost(mode, getValue().getIndex(), getDevice().getIndex(), compRes.nextEventQueue());
    }

}
