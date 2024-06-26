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
public class AlterISchemaRequest implements org.apache.thrift.TBase<AlterISchemaRequest, AlterISchemaRequest._Fields>, java.io.Serializable, Cloneable, Comparable<AlterISchemaRequest> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("AlterISchemaRequest");

  private static final org.apache.thrift.protocol.TField NAME_FIELD_DESC = new org.apache.thrift.protocol.TField("name", org.apache.thrift.protocol.TType.STRUCT, (short)1);
  private static final org.apache.thrift.protocol.TField NEW_SCHEMA_FIELD_DESC = new org.apache.thrift.protocol.TField("newSchema", org.apache.thrift.protocol.TType.STRUCT, (short)3);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new AlterISchemaRequestStandardSchemeFactory());
    schemes.put(TupleScheme.class, new AlterISchemaRequestTupleSchemeFactory());
  }

  private ISchemaName name; // required
  private ISchema newSchema; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    NAME((short)1, "name"),
    NEW_SCHEMA((short)3, "newSchema");

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
        case 1: // NAME
          return NAME;
        case 3: // NEW_SCHEMA
          return NEW_SCHEMA;
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
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.NAME, new org.apache.thrift.meta_data.FieldMetaData("name", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, ISchemaName.class)));
    tmpMap.put(_Fields.NEW_SCHEMA, new org.apache.thrift.meta_data.FieldMetaData("newSchema", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, ISchema.class)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(AlterISchemaRequest.class, metaDataMap);
  }

  public AlterISchemaRequest() {
  }

  public AlterISchemaRequest(
    ISchemaName name,
    ISchema newSchema)
  {
    this();
    this.name = name;
    this.newSchema = newSchema;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public AlterISchemaRequest(AlterISchemaRequest other) {
    if (other.isSetName()) {
      this.name = new ISchemaName(other.name);
    }
    if (other.isSetNewSchema()) {
      this.newSchema = new ISchema(other.newSchema);
    }
  }

  public AlterISchemaRequest deepCopy() {
    return new AlterISchemaRequest(this);
  }

  @Override
  public void clear() {
    this.name = null;
    this.newSchema = null;
  }

  public ISchemaName getName() {
    return this.name;
  }

  public void setName(ISchemaName name) {
    this.name = name;
  }

  public void unsetName() {
    this.name = null;
  }

  /** Returns true if field name is set (has been assigned a value) and false otherwise */
  public boolean isSetName() {
    return this.name != null;
  }

  public void setNameIsSet(boolean value) {
    if (!value) {
      this.name = null;
    }
  }

  public ISchema getNewSchema() {
    return this.newSchema;
  }

  public void setNewSchema(ISchema newSchema) {
    this.newSchema = newSchema;
  }

  public void unsetNewSchema() {
    this.newSchema = null;
  }

  /** Returns true if field newSchema is set (has been assigned a value) and false otherwise */
  public boolean isSetNewSchema() {
    return this.newSchema != null;
  }

  public void setNewSchemaIsSet(boolean value) {
    if (!value) {
      this.newSchema = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case NAME:
      if (value == null) {
        unsetName();
      } else {
        setName((ISchemaName)value);
      }
      break;

    case NEW_SCHEMA:
      if (value == null) {
        unsetNewSchema();
      } else {
        setNewSchema((ISchema)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case NAME:
      return getName();

    case NEW_SCHEMA:
      return getNewSchema();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case NAME:
      return isSetName();
    case NEW_SCHEMA:
      return isSetNewSchema();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof AlterISchemaRequest)
      return this.equals((AlterISchemaRequest)that);
    return false;
  }

  public boolean equals(AlterISchemaRequest that) {
    if (that == null)
      return false;

    boolean this_present_name = true && this.isSetName();
    boolean that_present_name = true && that.isSetName();
    if (this_present_name || that_present_name) {
      if (!(this_present_name && that_present_name))
        return false;
      if (!this.name.equals(that.name))
        return false;
    }

    boolean this_present_newSchema = true && this.isSetNewSchema();
    boolean that_present_newSchema = true && that.isSetNewSchema();
    if (this_present_newSchema || that_present_newSchema) {
      if (!(this_present_newSchema && that_present_newSchema))
        return false;
      if (!this.newSchema.equals(that.newSchema))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    List<Object> list = new ArrayList<Object>();

    boolean present_name = true && (isSetName());
    list.add(present_name);
    if (present_name)
      list.add(name);

    boolean present_newSchema = true && (isSetNewSchema());
    list.add(present_newSchema);
    if (present_newSchema)
      list.add(newSchema);

    return list.hashCode();
  }

  @Override
  public int compareTo(AlterISchemaRequest other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetName()).compareTo(other.isSetName());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetName()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.name, other.name);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetNewSchema()).compareTo(other.isSetNewSchema());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetNewSchema()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.newSchema, other.newSchema);
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
    StringBuilder sb = new StringBuilder("AlterISchemaRequest(");
    boolean first = true;

    sb.append("name:");
    if (this.name == null) {
      sb.append("null");
    } else {
      sb.append(this.name);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("newSchema:");
    if (this.newSchema == null) {
      sb.append("null");
    } else {
      sb.append(this.newSchema);
    }
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // check for sub-struct validity
    if (name != null) {
      name.validate();
    }
    if (newSchema != null) {
      newSchema.validate();
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

  private static class AlterISchemaRequestStandardSchemeFactory implements SchemeFactory {
    public AlterISchemaRequestStandardScheme getScheme() {
      return new AlterISchemaRequestStandardScheme();
    }
  }

  private static class AlterISchemaRequestStandardScheme extends StandardScheme<AlterISchemaRequest> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, AlterISchemaRequest struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // NAME
            if (schemeField.type == org.apache.thrift.protocol.TType.STRUCT) {
              struct.name = new ISchemaName();
              struct.name.read(iprot);
              struct.setNameIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // NEW_SCHEMA
            if (schemeField.type == org.apache.thrift.protocol.TType.STRUCT) {
              struct.newSchema = new ISchema();
              struct.newSchema.read(iprot);
              struct.setNewSchemaIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, AlterISchemaRequest struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.name != null) {
        oprot.writeFieldBegin(NAME_FIELD_DESC);
        struct.name.write(oprot);
        oprot.writeFieldEnd();
      }
      if (struct.newSchema != null) {
        oprot.writeFieldBegin(NEW_SCHEMA_FIELD_DESC);
        struct.newSchema.write(oprot);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class AlterISchemaRequestTupleSchemeFactory implements SchemeFactory {
    public AlterISchemaRequestTupleScheme getScheme() {
      return new AlterISchemaRequestTupleScheme();
    }
  }

  private static class AlterISchemaRequestTupleScheme extends TupleScheme<AlterISchemaRequest> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, AlterISchemaRequest struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetName()) {
        optionals.set(0);
      }
      if (struct.isSetNewSchema()) {
        optionals.set(1);
      }
      oprot.writeBitSet(optionals, 2);
      if (struct.isSetName()) {
        struct.name.write(oprot);
      }
      if (struct.isSetNewSchema()) {
        struct.newSchema.write(oprot);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, AlterISchemaRequest struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(2);
      if (incoming.get(0)) {
        struct.name = new ISchemaName();
        struct.name.read(iprot);
        struct.setNameIsSet(true);
      }
      if (incoming.get(1)) {
        struct.newSchema = new ISchema();
        struct.newSchema.read(iprot);
        struct.setNewSchemaIsSet(true);
      }
    }
  }

}

