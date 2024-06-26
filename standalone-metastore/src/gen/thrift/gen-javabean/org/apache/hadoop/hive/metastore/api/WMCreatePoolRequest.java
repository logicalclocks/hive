/**
 * Autogenerated by Thrift Compiler (0.9.3)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package org.apache.hadoop.hive.metastore.api;

import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;

import org.apache.thrift.scheme.TupleScheme;
import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.EncodingUtils;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.server.AbstractNonblockingServer.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Collections;
import java.util.BitSet;
import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.annotation.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked"})
@Generated(value = "Autogenerated by Thrift Compiler (0.9.3)")
public class WMCreatePoolRequest implements org.apache.thrift.TBase<WMCreatePoolRequest, WMCreatePoolRequest._Fields>, java.io.Serializable, Cloneable, Comparable<WMCreatePoolRequest> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("WMCreatePoolRequest");

  private static final org.apache.thrift.protocol.TField POOL_FIELD_DESC = new org.apache.thrift.protocol.TField("pool", org.apache.thrift.protocol.TType.STRUCT, (short)1);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new WMCreatePoolRequestStandardSchemeFactory());
    schemes.put(TupleScheme.class, new WMCreatePoolRequestTupleSchemeFactory());
  }

  private WMPool pool; // optional

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    POOL((short)1, "pool");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // POOL
          return POOL;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  private static final _Fields optionals[] = {_Fields.POOL};
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.POOL, new org.apache.thrift.meta_data.FieldMetaData("pool", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, WMPool.class)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(WMCreatePoolRequest.class, metaDataMap);
  }

  public WMCreatePoolRequest() {
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public WMCreatePoolRequest(WMCreatePoolRequest other) {
    if (other.isSetPool()) {
      this.pool = new WMPool(other.pool);
    }
  }

  public WMCreatePoolRequest deepCopy() {
    return new WMCreatePoolRequest(this);
  }

  @Override
  public void clear() {
    this.pool = null;
  }

  public WMPool getPool() {
    return this.pool;
  }

  public void setPool(WMPool pool) {
    this.pool = pool;
  }

  public void unsetPool() {
    this.pool = null;
  }

  /** Returns true if field pool is set (has been assigned a value) and false otherwise */
  public boolean isSetPool() {
    return this.pool != null;
  }

  public void setPoolIsSet(boolean value) {
    if (!value) {
      this.pool = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case POOL:
      if (value == null) {
        unsetPool();
      } else {
        setPool((WMPool)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case POOL:
      return getPool();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case POOL:
      return isSetPool();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof WMCreatePoolRequest)
      return this.equals((WMCreatePoolRequest)that);
    return false;
  }

  public boolean equals(WMCreatePoolRequest that) {
    if (that == null)
      return false;

    boolean this_present_pool = true && this.isSetPool();
    boolean that_present_pool = true && that.isSetPool();
    if (this_present_pool || that_present_pool) {
      if (!(this_present_pool && that_present_pool))
        return false;
      if (!this.pool.equals(that.pool))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    List<Object> list = new ArrayList<Object>();

    boolean present_pool = true && (isSetPool());
    list.add(present_pool);
    if (present_pool)
      list.add(pool);

    return list.hashCode();
  }

  @Override
  public int compareTo(WMCreatePoolRequest other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetPool()).compareTo(other.isSetPool());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetPool()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.pool, other.pool);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("WMCreatePoolRequest(");
    boolean first = true;

    if (isSetPool()) {
      sb.append("pool:");
      if (this.pool == null) {
        sb.append("null");
      } else {
        sb.append(this.pool);
      }
      first = false;
    }
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // check for sub-struct validity
    if (pool != null) {
      pool.validate();
    }
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class WMCreatePoolRequestStandardSchemeFactory implements SchemeFactory {
    public WMCreatePoolRequestStandardScheme getScheme() {
      return new WMCreatePoolRequestStandardScheme();
    }
  }

  private static class WMCreatePoolRequestStandardScheme extends StandardScheme<WMCreatePoolRequest> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, WMCreatePoolRequest struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // POOL
            if (schemeField.type == org.apache.thrift.protocol.TType.STRUCT) {
              struct.pool = new WMPool();
              struct.pool.read(iprot);
              struct.setPoolIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, WMCreatePoolRequest struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.pool != null) {
        if (struct.isSetPool()) {
          oprot.writeFieldBegin(POOL_FIELD_DESC);
          struct.pool.write(oprot);
          oprot.writeFieldEnd();
        }
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class WMCreatePoolRequestTupleSchemeFactory implements SchemeFactory {
    public WMCreatePoolRequestTupleScheme getScheme() {
      return new WMCreatePoolRequestTupleScheme();
    }
  }

  private static class WMCreatePoolRequestTupleScheme extends TupleScheme<WMCreatePoolRequest> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, WMCreatePoolRequest struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetPool()) {
        optionals.set(0);
      }
      oprot.writeBitSet(optionals, 1);
      if (struct.isSetPool()) {
        struct.pool.write(oprot);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, WMCreatePoolRequest struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(1);
      if (incoming.get(0)) {
        struct.pool = new WMPool();
        struct.pool.read(iprot);
        struct.setPoolIsSet(true);
      }
    }
  }

}

