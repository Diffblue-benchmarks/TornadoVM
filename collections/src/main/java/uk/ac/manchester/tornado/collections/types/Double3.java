/*
 * This file is part of Tornado: A heterogeneous programming framework: 
 * https://github.com/beehive-lab/tornado
 *
 * Copyright (c) 2013-2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Authors: James Clarkson
 *
 */
package uk.ac.manchester.tornado.collections.types;

import static java.lang.String.format;
import static java.nio.DoubleBuffer.wrap;
import static uk.ac.manchester.tornado.collections.types.DoubleOps.fmt3;

import java.nio.DoubleBuffer;

import uk.ac.manchester.tornado.api.Payload;
import uk.ac.manchester.tornado.api.Vector;
import uk.ac.manchester.tornado.collections.math.TornadoMath;

/**
 * Class that represents a vector of 3x doubles e.g. <double,double,double>
 *
 * @author jamesclarkson
 */
@Vector
public final class Double3 implements PrimitiveStorage<DoubleBuffer> {

    public static final Class<Double3> TYPE = Double3.class;

    /**
     * backing array
     */
    @Payload
    final protected double[] storage;

    /**
     * number of elements in the storage
     */
    final private static int numElements = 3;

    public Double3(double[] storage) {
        this.storage = storage;
    }

    public Double3() {
        this(new double[numElements]);
    }

    public Double3(double x, double y, double z) {
        this();
        setX(x);
        setY(y);
        setZ(z);
    }

    public double get(int index) {
        return storage[index];
    }

    public void set(int index, double value) {
        storage[index] = value;
    }

    public void set(Double3 value) {
        setX(value.getX());
        setY(value.getY());
        setZ(value.getZ());
    }

    public double getX() {
        return get(0);
    }

    public double getY() {
        return get(1);
    }

    public double getZ() {
        return get(2);
    }

    public double getS0() {
        return get(0);
    }

    public double getS1() {
        return get(1);
    }

    public double getS2() {
        return get(2);
    }

    public void setX(double value) {
        set(0, value);
    }

    public void setY(double value) {
        set(1, value);
    }

    public void setZ(double value) {
        set(2, value);
    }

    public void setS0(double value) {
        set(0, value);
    }

    public void setS1(double value) {
        set(1, value);
    }

    public void setS2(double value) {
        set(2, value);
    }

    /**
     * Duplicates this vector
     *
     * @return
     */
    public Double3 duplicate() {
        final Double3 vector = new Double3();
        vector.set(this);
        return vector;
    }

    public String toString(String fmt) {
        return format(fmt, getX(), getY(), getZ());
    }

    @Override
    public String toString() {
        return toString(fmt3);
    }

    /**
     * Cast vector into a Double2
     *
     * @return
     */
    public Double2 asDouble2() {
        return new Double2(getX(), getY());
    }

    protected static final Double3 loadFromArray(final double[] array, int index) {
        final Double3 result = new Double3();
        result.setX(array[index]);
        result.setY(array[index + 1]);
        result.setZ(array[index + 2]);
        return result;
    }

    protected final void storeToArray(final double[] array, int index) {
        array[index] = getX();
        array[index + 1] = getY();
        array[index + 2] = getZ();
    }

    @Override
    public void loadFromBuffer(DoubleBuffer buffer) {
        asBuffer().put(buffer);
    }

    @Override
    public DoubleBuffer asBuffer() {
        return wrap(storage);
    }

    @Override
    public int size() {
        return numElements;
    }

    /**
     * *
     * Operations on Double3 vectors
     */
    /*
     * vector = op( vector, vector )
     */
    public static Double3 add(Double3 a, Double3 b) {
        return new Double3(a.getX() + b.getX(), a.getY() + b.getY(), a.getZ() + b.getZ());
    }

    public static Double3 sub(Double3 a, Double3 b) {
        return new Double3(a.getX() - b.getX(), a.getY() - b.getY(), a.getZ() - b.getZ());
    }

    public static Double3 div(Double3 a, Double3 b) {
        return new Double3(a.getX() / b.getX(), a.getY() / b.getY(), a.getZ() / b.getZ());
    }

    public static Double3 mult(Double3 a, Double3 b) {
        return new Double3(a.getX() * b.getX(), a.getY() * b.getY(), a.getZ() * b.getZ());
    }

    public static Double3 min(Double3 a, Double3 b) {
        return new Double3(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
    }

    public static Double3 max(Double3 a, Double3 b) {
        return new Double3(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
    }

    public static Double3 cross(Double3 a, Double3 b) {
        return new Double3(
                a.getY() * b.getZ() - a.getZ() * b.getY(),
                a.getZ() * b.getX() - a.getX() * b.getZ(),
                a.getX() * b.getY() - a.getY() * b.getX());
    }

    /*
     * vector = op (vector, scalar)
     */
    public static Double3 add(Double3 a, double b) {
        return new Double3(a.getX() + b, a.getY() + b, a.getZ() + b);
    }

    public static Double3 sub(Double3 a, double b) {
        return new Double3(a.getX() - b, a.getY() - b, a.getZ() - b);
    }

    public static Double3 mult(Double3 a, double b) {
        return new Double3(a.getX() * b, a.getY() * b, a.getZ() * b);
    }

    public static Double3 div(Double3 a, double b) {
        return new Double3(a.getX() / b, a.getY() / b, a.getZ() / b);
    }

    public static Double3 inc(Double3 a, double value) {
        return new Double3(a.getX() + value,
                a.getY() + value,
                a.getZ() + value);
    }

    public static Double3 dec(Double3 a, double value) {
        return new Double3(a.getX() - value,
                a.getY() - value,
                a.getZ() - value);
    }

    public static Double3 scaleByInverse(Double3 a, double value) {
        return mult(a, 1f / value);
    }

    public static Double3 scale(Double3 a, double value) {
        return mult(a, value);
    }

    /*
     * vector = op(vector)
     */
    public static Double3 sqrt(Double3 a) {
        return new Double3(TornadoMath.sqrt(a.getX()), TornadoMath.sqrt(a.getY()), TornadoMath.sqrt(a.getZ()));
    }

    public static Double3 floor(Double3 a) {
        return new Double3(TornadoMath.floor(a.getX()), TornadoMath.floor(a.getY()), TornadoMath.floor(a.getZ()));
    }

    public static Double3 fract(Double3 a) {
        return new Double3(TornadoMath.fract(a.getX()), TornadoMath.fract(a.getY()), TornadoMath.fract(a.getZ()));
    }

    /*
     * misc inplace vector ops
     */
    public static Double3 clamp(Double3 x, double min, double max) {
        return new Double3(
                TornadoMath.clamp(x.getX(), min, max),
                TornadoMath.clamp(x.getY(), min, max),
                TornadoMath.clamp(x.getZ(), min, max));
    }

//	public static void normalise(Double3 value){
//		final double len = length(value);
//		scaleByInverse(value, len);
//	}
    public static Double3 normalise(Double3 value) {
        final double len = 1f / length(value);
        return mult(value, len);
    }

    /*
     * vector wide operations
     */
    public static double min(Double3 value) {
        return Math.min(value.getX(), Math.min(value.getY(), value.getZ()));
    }

    public static double max(Double3 value) {
        return Math.max(value.getX(), Math.max(value.getY(), value.getZ()));
    }

    public static double dot(Double3 a, Double3 b) {
        final Double3 m = mult(a, b);
        return m.getX() + m.getY() + m.getZ();
    }

    /**
     * Returns the vector length e.g. the sqrt of all elements squared
     *
     * @return
     */
    public static double length(Double3 value) {
        return TornadoMath.sqrt(dot(value, value));
    }

    public static boolean isEqual(Double3 a, Double3 b) {
        return TornadoMath.isEqual(a.asBuffer().array(), b.asBuffer().array());
    }

    public static boolean isEqualULP(Double3 a, Double3 b, double numULP) {
        return TornadoMath.isEqualULP(a.asBuffer().array(), b.asBuffer().array(), numULP);
    }

    public static double findULPDistance(Double3 a, Double3 b) {
        return TornadoMath.findULPDistance(a.asBuffer().array(), b.asBuffer().array());
    }
}