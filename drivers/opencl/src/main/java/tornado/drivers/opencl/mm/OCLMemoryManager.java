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
package tornado.drivers.opencl.mm;

import java.lang.reflect.Method;
import tornado.api.Parallel;
import tornado.api.meta.ScheduleMetaData;
import tornado.api.meta.TaskMetaData;
import tornado.common.CachedObject;
import tornado.common.TornadoLogger;
import tornado.common.TornadoMemoryProvider;
import tornado.common.exceptions.TornadoOutOfMemoryException;
import tornado.drivers.opencl.OCLDeviceContext;
import tornado.drivers.opencl.graal.OCLInstalledCode;
import tornado.drivers.opencl.graal.backend.OCLBackend;
import tornado.meta.domain.DomainTree;
import tornado.meta.domain.IntDomain;
import tornado.runtime.api.TornadoCallStack;
import tornado.runtime.cache.TornadoByteBuffer;

import static tornado.common.RuntimeUtilities.humanReadableByteCount;
import static tornado.common.exceptions.TornadoInternalError.guarantee;
import static tornado.drivers.opencl.enums.OCLMemFlags.CL_MEM_READ_WRITE;
import static tornado.runtime.api.TornadoCallStack.RESERVED_SLOTS;

public class OCLMemoryManager extends TornadoLogger implements TornadoMemoryProvider {

    private final ScheduleMetaData scheduleMeta;
    private final long callStackLimit;
    private long callStackPosition;
    private long deviceBufferAddress;
    private final OCLDeviceContext deviceContext;

    private long buffer;
    private long heapLimit;

    private long heapPosition;

    private boolean initialised;

    private OCLInstalledCode initFP64Code;
    private OCLInstalledCode initFP32Code;
    private OCLInstalledCode initU32Code;
    private TornadoCallStack initCallStack;
    private DomainTree initThreads;
    private TaskMetaData initMeta;

    public OCLMemoryManager(final OCLDeviceContext device) {

        deviceContext = device;
        callStackLimit = 8192;
        initialised = false;
//        System.out.printf("device id %s\n", device.getId());
        scheduleMeta = new ScheduleMetaData("mm-" + device.getDeviceId());

        reset();
    }

    @Override
    public long getCallStackAllocated() {
        return callStackPosition;
    }

    @Override
    public long getCallStackRemaining() {
        return callStackLimit - callStackPosition;
    }

    @Override
    public long getCallStackSize() {
        return callStackLimit;
    }

    @Override
    public long getHeapAllocated() {
        return heapPosition - callStackLimit;
    }

    @Override
    public long getHeapRemaining() {
        return heapLimit - heapPosition;
    }

    @Override
    public long toAbsoluteAddress(TornadoByteBuffer buffer) {
        return toAbsoluteDeviceAddress(buffer.getBufferOffset());
    }

    @Override
    public long toRelativeAddress(TornadoByteBuffer buffer) {
        return buffer.getBufferOffset();
    }

    @Override
    public long toAbsoluteAddress(CachedObject object) {
        return toAbsoluteDeviceAddress(object.getBufferOffset());
    }

    @Override
    public long toRelativeAddress(CachedObject object) {
        return object.getBufferOffset();
    }

    private void initFP64(double[] data, int count) {
        for (@Parallel int i = 0; i < count; i++) {
            data[i] = 0;
        }
    }

    private void initFP32(float[] data, int count) {
        for (@Parallel int i = 0; i < count; i++) {
            data[i] = -1f;
        }
    }

    private void initU32(int[] data, int count) {
        for (@Parallel int i = 0; i < count; i++) {
            data[i] = 0;
        }
    }

    private Method getMethod(final String name, Class<?> type1) {
        Method method = null;
        try {
            method = this.getClass().getDeclaredMethod(name, type1, int.class);
            method.setAccessible(true);
        } catch (NoSuchMethodException | SecurityException e) {
            fatal("unable to find " + name + " method: " + e.getMessage());
        }
        return method;
    }

    private void createMemoryInitializers(final OCLBackend backend) {
//        initThreads = new DomainTree(1);
//        initMeta = new OCLMeta(2);
//        initMeta.addProvider(TornadoDevice.class, backend.getDeviceContext().asMapping());

//    	initFP64Code = OCLCompiler.compileCodeForDevice(
//				TornadoRuntime.resolveMethod(getMethod("initFP64",double[].class)), null, initMeta, (OCLProviders) backend.getProviders(), backend);
//        initFP32Code = OCLCompiler.compileCodeForDevice(
//                getTornadoRuntime().resolveMethod(getMethod("initFP32", float[].class)), null, initMeta, (OCLProviders) backend.getProviders(), backend);
//        initU32Code = OCLCompiler.compileCodeForDevice(
//                getTornadoRuntime().resolveMethod(getMethod("initU32", int[].class)), null, initMeta, (OCLProviders) backend.getProviders(), backend);
        initCallStack = createCallStack(4);

    }

    @Override
    public final void reset() {
        callStackPosition = 0;
        heapPosition = callStackLimit;
        info("Reset heap @ 0x%x (%s) on %s", deviceBufferAddress,
                humanReadableByteCount(heapLimit, true),
                deviceContext.getDevice().getName());
    }

    @Override
    public long getHeapSize() {
        return heapLimit - callStackLimit;
    }

    private static long align(final long address, final long alignment) {
        return (address % alignment == 0) ? address : address
                + (alignment - address % alignment);
    }

    public long tryAllocate(final Class<?> type, final long bytes, final int headerSize, int alignment)
            throws TornadoOutOfMemoryException {
        final long alignedDataStart = align(heapPosition + headerSize, alignment);
        final long headerStart = alignedDataStart - headerSize;
        if (headerStart + bytes < heapLimit) {
            heapPosition = headerStart + bytes;

//            final long byteCount = bytes - headerSize;
//            if(type != null && type.isArray() && RuntimeUtilities.isPrimitiveArray(type)){
//            	if(type == double[].class ){
//            		initialiseMemory(initFP64Code,offset + headerSize, (int) (byteCount / 8));
//            	} else if(type == float[].class){
//            		initialiseMemory(initFP32Code,offset + headerSize, (int) (byteCount / 4));
//            	} else {
//            		TornadoInternalError.guarantee(byteCount % 4 == 0, "array is not divisible by 4");
//            		initialiseMemory(initU32Code,offset + headerSize, (int) (byteCount / 4));
//            	}
//            } else {
//            	TornadoInternalError.guarantee(byteCount % 4 == 0, "array is not divisible by 4");
//            	initialiseMemory(initU32Code,offset + headerSize, (int) (byteCount/4));
//            }
        } else {
            throw new TornadoOutOfMemoryException("Out of memory on device: "
                    + deviceContext.getDevice().getName());
        }

        return headerStart;
    }

    private void initialiseMemory(OCLInstalledCode code, long offset, int count) {
        if (count <= 0) {
            return;
        }

        initCallStack.reset();

        initCallStack.putLong(offset);
        initCallStack.putInt(count);

        final TaskMetaData meta = new TaskMetaData(scheduleMeta, "init-call-stack", 0);
        initThreads.set(0, new IntDomain(0, 1, count));
        meta.setDomain(initThreads);

        code.execute(initCallStack, meta);
    }

    public TornadoCallStack createCallStack(final int maxArgs) {
        final long size = (maxArgs + RESERVED_SLOTS) << 3;
        TornadoCallStack callStack = new TornadoCallStack(maxArgs, buffer, callStackPosition, size,
                this.toAbsoluteAddress(), deviceContext.getByteOrder(),
                this::tryAllocate, deviceContext::enqueueBarrier,
                deviceContext::writeBuffer, deviceContext::enqueueWriteBuffer,
                deviceContext::readBuffer, deviceContext::enqueueReadBuffer);

        if (callStackPosition + callStack.getSize() < callStackLimit) {
            callStackPosition = align(callStackPosition + callStack.getSize(),
                    32);
        } else {
            fatal("Out of call-stack memory on %s\n\tused=%s, free=%s, required=%s",
                    deviceContext.getDevice().getName(),
                    humanReadableByteCount(callStackPosition, false),
                    humanReadableByteCount(callStackLimit - callStackPosition, false),
                    humanReadableByteCount(callStack.getSize(), false));
            callStack = null;
            System.exit(-1);
        }

        return callStack;
    }

    public long getBytesRemaining() {
        return heapLimit - heapPosition;
    }

    /**
     * *
     * Returns sub-buffer that can be use to access a region managed by the
     * memory manager.
     *
     * @param offset offset within the memory managers heap
     * @param length size in bytes of the sub-buffer
     *
     * @return
     */
    public TornadoByteBuffer getSubBuffer(final int offset, final int length) {
        return new TornadoByteBuffer(buffer, offset, length,
                this.toAbsoluteAddress(), deviceContext.getByteOrder(),
                this::tryAllocate, deviceContext::enqueueBarrier,
                deviceContext::writeBuffer, deviceContext::enqueueWriteBuffer,
                deviceContext::readBuffer, deviceContext::enqueueReadBuffer);
    }

    public void allocateRegion(long numBytes) {
        /*
         * Allocate space on the device
         */
        heapLimit = numBytes;
        buffer = deviceContext.getPlatformContext().createBuffer(
                CL_MEM_READ_WRITE, numBytes);
    }

    public void init(OCLBackend backend, long address) {
        deviceBufferAddress = address;
        initialised = true;
        info("Located heap @ 0x%x (%s) on %s", deviceBufferAddress,
                humanReadableByteCount(heapLimit, false),
                deviceContext.getDevice().getName());

        scheduleMeta.setDevice(backend.getDeviceContext().asMapping());
//        createMemoryInitializers(backend);
    }

    public long toAbsoluteAddress() {
        return deviceBufferAddress;
    }

    private long toAbsoluteDeviceAddress(final long address) {
        long result = address;

        guarantee(address + deviceBufferAddress >= 0, "absolute address may have wrapped arround: %d + %d = %d", address, deviceBufferAddress, address + deviceBufferAddress);
        result += deviceBufferAddress;

        return result;
    }

    public long toBuffer() {
        return buffer;
    }

    public long toRelativeAddress() {
        return 0;
    }

    private long toRelativeDeviceAddress(final long address) {
        long result = address;
//        guarantee(address - deviceBufferAddress < 0, "relative address may have wrapped arround: %d + %d = %d", address,deviceBufferAddress,address+deviceBufferAddress);
        if (!(Long.compareUnsigned(address, deviceBufferAddress) < 0 || Long
                .compareUnsigned(address, (deviceBufferAddress + heapLimit)) > 0)) {
            result -= deviceBufferAddress;
        }
        return result;
    }

    @Override
    public boolean isInitialised() {
        return initialised;
    }
}
