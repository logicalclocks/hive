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
public class AddDefaultConstraintRequest implements org.apache.thrift.TBase<AddDefaultConstraintRequest, AddDefaultConstraintRequest._Fields>, java.io.Serializable, Cloneable, Comparable<AddDefaultConstraintRequest> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("AddDefaultConstraintRequest");

  private static final org.apache.thrift.protocol.TField DEFAULT_CONSTRAINT_COLS_FIELD_DESC = new org.apache.thrift.protocol.TField("defaultConstraintCols", org.apache.thrift.protocol.TType.LIST, (short)1);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new AddDefaultConstraintRequestStandardSchemeFactory());
    schemes.put(TupleScheme.class, new AddDefaultConstraintRequestTupleSchemeFactory());
  }

  private List<SQLDefaultConstraint> defaultConstraintCols; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    DEFAULT_CONSTRAINT_COLS((short)1, "defaultConstraintCols");

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
        case 1: // DEFAULT_CONSTRAINT_COLS
          return DEFAULT_CONSTRAINT_COLS;
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
    tmpMap.put(_Fields.DEFAULT_CONSTRAINT_COLS, new org.apache.thrift.meta_data.FieldMetaData("defaultConstraintCols", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, SQLDefaultConstraint.class))));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(AddDefaultConstraintRequest.class, metaDataMap);
  }

  public AddDefaultConstraintRequest() {
  }

  public AddDefaultConstraintRequest(
    List<SQLDefaultConstraint> defaultConstraintCols)
  {
    this();
    this.defaultConstraintCols = defaultConstraintCols;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public AddDefaultConstraintRequest(AddDefaultConstraintRequest other) {
    if (other.isSetDefaultConstraintCols()) {
      List<SQLDefaultConstraint> __this__defaultConstraintCols = new ArrayList<SQLDefaultConstraint>(other.defaultConstraintCols.size());
      for (SQLDefaultConstraint other_element : other.defaultConstraintCols) {
        __this__defaultConstraintCols.add(new SQLDefaultConstraint(other_element));
      }
      this.defaultConstraintCols = __this__defaultConstraintCols;
    }
  }

  public AddDefaultConstraintRequest deepCopy() {
    return new AddDefaultConstraintRequest(this);
  }

  @Override
  public void clear() {
    this.defaultConstraintCols = null;
  }

  public int getDefaultConstraintColsSize() {
    return (this.defaultConstraintCols == null) ? 0 : this.defaultConstraintCols.size();
  }

  public java.util.Iterator<SQLDefaultConstraint> getDefaultConstraintColsIterator() {
    return (this.defaultConstraintCols == null) ? null : this.defaultConstraintCols.iterator();
  }

  public void addToDefaultConstraintCols(SQLDefaultConstraint elem) {
    if (this.defaultConstraintCols == null) {
      this.defaultConstraintCols = new ArrayList<SQLDefaultConstraint>();
    }
    this.defaultConstraintCols.add(elem);
  }

  public List<SQLDefaultConstraint> getDefaultConstraintCols() {
    return this.defaultConstraintCols;
  }

  public void setDefaultConstraintCols(List<SQLDefaultConstraint> defaultConstraintCols) {
    this.defaultConstraintCols = defaultConstraintCols;
  }

  public void unsetDefaultConstraintCols() {
    this.defaultConstraintCols = null;
  }

  /** Returns true if field defaultConstraintCols is set (has been assigned a value) and false otherwise */
  public boolean isSetDefaultConstraintCols() {
    return this.defaultConstraintCols != null;
  }

  public void setDefaultConstraintColsIsSet(boolean value) {
    if (!value) {
      this.defaultConstraintCols = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case DEFAULT_CONSTRAINT_COLS:
      if (value == null) {
        unsetDefaultConstraintCols();
      } else {
        setDefaultConstraintCols((List<SQLDefaultConstraint>)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case DEFAULT_CONSTRAINT_COLS:
      return getDefaultConstraintCols();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case DEFAULT_CONSTRAINT_COLS:
      return isSetDefaultConstraintCols();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof AddDefaultConstraintRequest)
      return this.equals((AddDefaultConstraintRequest)that);
    return false;
  }

  public boolean equals(AddDefaultConstraintRequest that) {
    if (that == null)
      return false;

    boolean this_present_defaultConstraintCols = true && this.isSetDefaultConstraintCols();
    boolean that_present_defaultConstraintCols = true && that.isSetDefaultConstraintCols();
    if (this_present_defaultConstraintCols || that_present_defaultConstraintCols) {
      if (!(this_present_defaultConstraintCols && that_present_defaultConstraintCols))
        return false;
      if (!this.defaultConstraintCols.equals(that.defaultConstraintCols))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    List<Object> list = new ArrayList<Object>();

    boolean present_defaultConstraintCols = true && (isSetDefaultConstraintCols());
    list.add(present_defaultConstraintCols);
    if (present_defaultConstraintCols)
      list.add(defaultConstraintCols);

    return list.hashCode();
  }

  @Override
  public int compareTo(AddDefaultConstraintRequest other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetDefaultConstraintCols()).compareTo(other.isSetDefaultConstraintCols());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetDefaultConstraintCols()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.defaultConstraintCols, other.defaultConstraintCols);
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
    StringBuilder sb = new StringBuilder("AddDefaultConstraintRequest(");
    boolean first = true;

    sb.append("defaultConstraintCols:");
    if (this.defaultConstraintCols == null) {
      sb.append("null");
    } else {
      sb.append(this.defaultConstraintCols);
    }
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    if (!isSetDefaultConstraintCols()) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'defaultConstraintCols' is unset! Struct:" + toString());
    }

    // check for sub-struct validity
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

  private static class AddDefaultConstraintRequestStandardSchemeFactory implements SchemeFactory {
    public AddDefaultConstraintRequestStandardScheme getScheme() {
      return new AddDefaultConstraintRequestStandardScheme();
    }
  }

  private static class AddDefaultConstraintRequestStandardScheme extends StandardScheme<AddDefaultConstraintRequest> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, AddDefaultConstraintRequest struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // DEFAULT_CONSTRAINT_COLS
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list400 = iprot.readListBegin();
                struct.defaultConstraintCols = new ArrayList<SQLDefaultConstraint>(_list400.size);
                SQLDefaultConstraint _elem401;
                for (int _i402 = 0; _i402 < _list400.size; ++_i402)
                {
                  _elem401 = new SQLDefaultConstraint();
                  _elem401.read(iprot);
                  struct.defaultConstraintCols.add(_elem401);
                }
                iprot.readListEnd();
              }
              struct.setDefaultConstraintColsIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, AddDefaultConstraintRequest struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.defaultConstraintCols != null) {
        oprot.writeFieldBegin(DEFAULT_CONSTRAINT_COLS_FIELD_DESC);
        {
          oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.defaultConstraintCols.size()));
          for (SQLDefaultConstraint _iter403 : struct.defaultConstraintCols)
          {
            _iter403.write(oprot);
          }
          oprot.writeListEnd();
        }
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class AddDefaultConstraintRequestTupleSchemeFactory implements SchemeFactory {
    public AddDefaultConstraintRequestTupleScheme getScheme() {
      return new AddDefaultConstraintRequestTupleScheme();
    }
  }

  private static class AddDefaultConstraintRequestTupleScheme extends TupleScheme<AddDefaultConstraintRequest> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, AddDefaultConstraintRequest struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      {
        oprot.writeI32(struct.defaultConstraintCols.size());
        for (SQLDefaultConstraint _iter404 : struct.defaultConstraintCols)
        {
          _iter404.write(oprot);
        }
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, AddDefaultConstraintRequest struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      {
        org.apache.thrift.protocol.TList _list405 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
        struct.defaultConstraintCols = new ArrayList<SQLDefaultConstraint>(_list405.size);
        SQLDefaultConstraint _elem406;
        for (int _i407 = 0; _i407 < _list405.size; ++_i407)
        {
          _elem406 = new SQLDefaultConstraint();
          _elem406.read(iprot);
          struct.defaultConstraintCols.add(_elem406);
        }
      }
      struct.setDefaultConstraintColsIsSet(true);
    }
  }

}

