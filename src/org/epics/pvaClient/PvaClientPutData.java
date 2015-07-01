/**
 * 
 */
package org.epics.pvaClient;

import org.epics.pvdata.property.Alarm;
import org.epics.pvdata.property.*;
import org.epics.pvdata.pv.PVArray;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVScalarArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.*;
import org.epics.pvdata.misc.*;
import org.epics.pvdata.factory.*;
import org.epics.pvdata.factory.PVDataFactory;

/**
 * This is a convenience wrapper for a PVStructure.
 * @author mrk
 *
 */
public class PvaClientPutData {
    static PvaClientPutData create(Structure structure) {
        return new PvaClientPutData(structure);
    }

    public PvaClientPutData(Structure structure)
    {
        this.structure = structure;
        pvStructure = pvDataCreate.createPVStructure(structure);
        int nfields = pvStructure.getNumberFields();
        bitSet = new BitSet(nfields);
        postHandler = new PvaClientPostHandler[nfields];
        PVField pvField;
        for(int i=0; i<nfields; ++i)
        {
            postHandler[i] = new PvaClientPostHandler(this,i);
            if(i==0) {
                pvField = pvStructure;
            } else {
                 pvField = pvStructure.getSubField(i);
            }
            pvField.setPostHandler(postHandler[i]);
        }
        pvValue = pvStructure.getSubField("value");
    }

    private static class PvaClientPostHandler implements PostHandler
    {
        private PvaClientPutData easyData;
        private int fieldNumber;
        public PvaClientPostHandler(PvaClientPutData easyData,int fieldNumber)
        {this.easyData = easyData; this.fieldNumber = fieldNumber;}
        public void postPut() { easyData.postPut(fieldNumber);}
    };

    private static final Convert convert = ConvertFactory.getConvert();
    private static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    private static final String noValue = "no value field";
    private static final String noScalar = "value is not a scalar" ;
    private static final String notCompatibleScalar = "value is not a compatible scalar" ;
    private static final String noArray = "value is not an array" ;
    private static final String noScalarArray = "value is not a scalarArray" ;
    private static final String notDoubleArray = "value is not a doubleArray" ;
    private static final String notStringArray = "value is not a stringArray" ;


    private PvaClientPostHandler[] postHandler;
    private final Structure structure;
    private PVStructure pvStructure = null;
    private BitSet bitSet = null;

    private String messagePrefix = "";
    private PVField pvValue;
    
    private final DoubleArrayData doubleArrayData = new DoubleArrayData();
    private final StringArrayData stringArrayData = new StringArrayData();

    private void checkValue()
    {
        if(pvValue!=null) return;
        throw new RuntimeException(messagePrefix + noValue);
    }

    private void postPut(int fieldNumber)
    {
        bitSet.set(fieldNumber);
    }

    /**
     * Set a prefix for throw messages.
     * @param value The prefix.
     */
    void setMessagePrefix(String value)
    {
        messagePrefix = value + " ";
    }

    /** Get the structure.
     * @return the structure.
     */
    Structure getStructure()
    {
        return structure;
    }

    /** Get the pvStructure.
     * @return the pvStructure.
     */
    PVStructure getPVStructure()
    {
        return pvStructure;
    }

    /** Get the BitSet for the pvStructure
     * This shows which fields have changed value.
     * @return The bitSet
     */
    BitSet getBitSet()
    {
        return bitSet;
    }

    /**
     * Show fields that have changed value, i. e. all fields as shown by bitSet.
     * @return The changed fields.
     */
    String showChanged()
    {
        String result = "";
        PVField pvField;
        int nextSet = bitSet.nextSetBit(0);
        while(nextSet>=0) {
            if(nextSet==0) {
                pvField = pvStructure;
            } else {
                pvField = pvStructure.getSubField(nextSet);
            }
            result += pvField.getFullName() + " = " + pvField + "\n";
            nextSet = bitSet.nextSetBit(nextSet);
        }
        return result;
    }

    /**
     * Is there a top level field named value of the PVstructure returned by channelGet?
     * @return The answer.
     */
    boolean hasValue()
    {
        if(pvValue==null) return false;
        return true;
    }

    /**
     * Is the value field a scalar?
     * @return The answer.
     */
    boolean isValueScalar()
    {
        if(pvValue==null) return false;
        if(pvValue.getField().getType()==Type.scalar) return true;
        return false;
    }

    /**
     * Is the value field a scalar array?
     * @return The answer.
     */
    boolean isValueScalarArray()
    {
        if(pvValue==null) return false;
        if(pvValue.getField().getType()==Type.scalarArray) return true;
        return false;
    }

    /**
     * Return the interface to the value field.
     * @return The interface or null if no top level value field.
     */
    PVField getValue()
    {
        checkValue();
        return pvValue;
    }

    /**
     * Return the interface to a scalar value field.
     * @return Return the interface for a scalar value field or null if no scalar value field.
     */
    PVScalar getScalarValue()
    {
        checkValue();
        PVScalar pv = pvStructure.getSubField(PVScalar.class,"value");
        if(pv==null) throw new RuntimeException(messagePrefix  + noScalar);
        return pv;
    }

    /**
     * Return the interface to an array value field.
     * @return Return the interface or null if an array value field does not exist.
     */
    PVArray getArrayValue()
    {

        checkValue();
        PVArray pv = pvStructure.getSubField(PVArray.class,"value");
        if(pv==null) throw new RuntimeException(messagePrefix  + noArray);
        return pv;
    }

    /**
     * Return the interface to a scalar array value field.
     * @return Return the interface or null if a scalar array value field does not exist
     */
    PVScalarArray getScalarArrayValue()
    {
        checkValue();
        PVScalarArray pv = pvStructure.getSubField(PVScalarArray.class,"value");
        if(pv==null) throw new RuntimeException(messagePrefix  + noScalarArray);
        return pv;
    }

    /**
     * Get the value as a double.
     * @return  If value is not a numeric scalar setStatus is called and 0 is returned.
     */
    double getDouble()
    {
        PVScalar pvScalar = getScalarValue();
        ScalarType scalarType = pvScalar.getScalar().getScalarType();
        if(scalarType==ScalarType.pvDouble) {
            PVDouble pv = (PVDouble)pvScalar;
            return pv.get();

        }
        if(!scalarType.isNumeric()) throw new RuntimeException(
                messagePrefix  + notCompatibleScalar);
        return convert.toDouble(pvScalar);
    }

    /**
     * Get the value as a string.
     * @return If value is not a scalar setStatus is called and 0 is returned.
     */
    String getString()
    {
        PVScalar pvScalar = getScalarValue();
        return convert.toString(pvScalar);
    }

    /**
     * Get the value as a double array.
     * @return If the value is not a numeric array null is returned. 
     */
    double[] getDoubleArray()
    {
        checkValue();
        PVDoubleArray pvDoubleArray = pvStructure.getSubField(
                PVDoubleArray.class,"value");
        if(pvDoubleArray==null) throw new RuntimeException(
                messagePrefix  + notDoubleArray);
        int length = pvDoubleArray.getLength();
        double[] data = new double[length];
        pvDoubleArray.get(0, length, doubleArrayData);
        for(int i=0; i<length; ++i) data[i] = doubleArrayData.data[i];
        return data;
    }

    /**
     * Get the value as a string array.
     * @return If the value is not a scalar array null is returned.
     */
    String[] getStringArray()
    {
        checkValue();
        PVStringArray pvStringArray = pvStructure.getSubField(
                PVStringArray.class,"value");
        if(pvStringArray==null) throw new RuntimeException(
                messagePrefix  + notStringArray);
        int length = pvStringArray.getLength();
        String[] data = new String[length];
        pvStringArray.get(0, length, stringArrayData);
        for(int i=0; i<length; ++i) data[i] = stringArrayData.data[i];
        return data;
    }

    /**
     * Copy a sub-array of the value field into value.
     * If the value field is not a numeric array field no elements are copied.
     * @param value The place where data is copied.
     * @param length The maximum number of elements to copy.
     * @return The number of elements copied.
     */
    int getDoubleArray(double[] value,int length)
    {
        checkValue();
        PVDoubleArray pvDoubleArray = pvStructure.getSubField(
                PVDoubleArray.class,"value");
        if(pvDoubleArray==null) throw new RuntimeException(
                messagePrefix  + notDoubleArray);
        if(pvDoubleArray.getLength()<length) length 
        = pvDoubleArray.getLength();            
        pvDoubleArray.get(0, length, doubleArrayData);
        for(int i=0; i<length; ++i) value[i] = doubleArrayData.data[i];
        return length;
    }

    /**
     * Copy a sub-array of the value field into value.
     * If the value field is not a scalar array field no elements are copied.
     * @param value The place where data is copied.
     * @param length The maximum number of elements to copy.
     * @return The number of elements copied.
     */
    int getStringArray(String[] value,int length)
    {
        checkValue();
        PVStringArray pvStringArray = pvStructure.getSubField(
                PVStringArray.class,"value");
        if(pvStringArray==null) throw new RuntimeException(
                messagePrefix  + notStringArray);
        if(pvStringArray.getLength()<length) length
        = pvStringArray.getLength();            
        pvStringArray.get(0, length, stringArrayData);
        for(int i=0; i<length; ++i) value[i] = stringArrayData.data[i];
        return length;
    }

    /**
     * Put the value as a double.
     * An exception is also thrown if the actualy type can cause an overflow.
     * If value is not a numeric scalar an exception is thrown.
     */
    void putDouble(double value)
    {
        PVScalar pvScalar = getScalarValue();
        ScalarType scalarType = pvScalar.getScalar().getScalarType();
        if(scalarType==ScalarType.pvDouble) {
            PVDouble pvDouble = (PVDouble)(pvScalar);
            pvDouble.put(value);
            return;
        }
        if(!scalarType.isNumeric()) throw new RuntimeException(messagePrefix + notCompatibleScalar);
        convert.fromDouble(pvScalar,value);
    }

    /**
     * Put the value as a string.
     * If value is not a  scalar an exception is thrown.
     */
    void putString(String value)
    {
         PVScalar pvScalar = getScalarValue();
         convert.fromString(pvScalar,value);
    }

    /**
     * Copy the array to the value field.
     * If the value field is not a double array field an exception is thrown.
     * @param value The place where data is copied.
     */
    void putDoubleArray(double[] value)
    {
        checkValue();
        PVDoubleArray pv = pvStructure.getSubField(PVDoubleArray.class,"value");
        convert.fromDoubleArray(pv, 0, value.length, value, 0);
    }

    /**
     * Copy array to the value field.
     * If the value field is not a string array field an exception is thrown.
     * @param value data source
     */
    void putStringArray(String[] value)
    {
        checkValue();
        PVStringArray pv = pvStructure.getSubField(PVStringArray.class,"value");
        convert.fromStringArray(pv, 0, value.length, value, 0);
    }

}
