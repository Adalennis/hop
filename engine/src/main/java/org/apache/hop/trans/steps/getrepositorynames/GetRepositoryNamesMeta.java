/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2017 by Hitachi Vantara : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.apache.hop.trans.steps.getrepositorynames;

import java.util.List;

import org.apache.hop.core.CheckResult;
import org.apache.hop.core.CheckResultInterface;
import org.apache.hop.core.Const;
import org.apache.hop.core.database.DatabaseMeta;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopStepException;
import org.apache.hop.core.exception.HopXMLException;
import org.apache.hop.core.row.RowMetaInterface;
import org.apache.hop.core.row.ValueMetaInterface;
import org.apache.hop.core.row.value.ValueMetaDate;
import org.apache.hop.core.row.value.ValueMetaInteger;
import org.apache.hop.core.row.value.ValueMetaString;
import org.apache.hop.core.variables.VariableSpace;
import org.apache.hop.core.xml.XMLHandler;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.repository.ObjectId;
import org.apache.hop.repository.Repository;
import org.apache.hop.trans.Trans;
import org.apache.hop.trans.TransMeta;
import org.apache.hop.trans.step.BaseStepMeta;
import org.apache.hop.trans.step.StepDataInterface;
import org.apache.hop.trans.step.StepInterface;
import org.apache.hop.trans.step.StepMeta;
import org.apache.hop.trans.step.StepMetaInterface;
import org.apache.hop.metastore.api.IMetaStore;
import org.w3c.dom.Node;

public class GetRepositoryNamesMeta extends BaseStepMeta implements StepMetaInterface {
  private static Class<?> PKG = GetRepositoryNamesMeta.class; // i18n!

  /** Array of directories */
  private String[] directory;

  /** Name wild card (regular expression) */
  private String[] nameMask;

  /** Name Wild card to exclude (regular expression) */
  private String[] excludeNameMask;

  /**
   * Array of boolean values as string, indicating if we need to fetch sub folders.
   */
  private boolean[] includeSubFolders;

  /** Filter indicating object type filter */
  private ObjectTypeSelection objectTypeSelection;

  /** Flag indicating that a row number field should be included in the output */
  private boolean includeRowNumber;

  /** The name of the field in the output containing the row number */
  private String rowNumberField;

  public GetRepositoryNamesMeta() {
    super(); // allocate BaseStepMeta

    objectTypeSelection = ObjectTypeSelection.All;
  }

  /**
   * @return Returns the rowNumberField.
   */
  public String getRowNumberField() {
    return rowNumberField;
  }

  public Object clone() {
    GetRepositoryNamesMeta retval = (GetRepositoryNamesMeta) super.clone();

    int nrfiles = directory.length;

    retval.allocate( nrfiles );

    System.arraycopy( directory, 0, retval.directory, 0, nrfiles );
    System.arraycopy( nameMask, 0, retval.nameMask, 0, nrfiles );
    System.arraycopy( excludeNameMask, 0, retval.excludeNameMask, 0, nrfiles );
    System.arraycopy( includeSubFolders, 0, retval.includeSubFolders, 0, nrfiles );

    return retval;
  }

  public void allocate( int nrfiles ) {
    directory = new String[nrfiles];
    nameMask = new String[nrfiles];
    excludeNameMask = new String[nrfiles];
    includeSubFolders = new boolean[nrfiles];
  }

  public void setDefault() {
    objectTypeSelection = ObjectTypeSelection.All;
    includeRowNumber = true;
    rowNumberField = "rownr";

    int nrfiles = 1;
    allocate( nrfiles );

    for ( int i = 0; i < nrfiles; i++ ) {
      directory[i] = "/";
      nameMask[i] = ".*";
      excludeNameMask[i] = "";
      includeSubFolders[i] = true;
    }
  }

  public void getFields( RowMetaInterface row, String origin, RowMetaInterface[] info, StepMeta nextStep,
    VariableSpace space, Repository repository, IMetaStore metaStore ) throws HopStepException {

    // the directory and name of the object
    //
    ValueMetaInterface object = new ValueMetaString( "object" );
    object.setLength( 500 );
    object.setPrecision( -1 );
    object.setOrigin( origin );
    row.addValueMeta( object );

    // the directory
    //
    ValueMetaInterface directory = new ValueMetaString( "directory" );
    directory.setLength( 500 );
    directory.setPrecision( -1 );
    directory.setOrigin( origin );
    row.addValueMeta( directory );

    // the name
    //
    ValueMetaInterface name = new ValueMetaString( "name" );
    name.setLength( 500 );
    name.setPrecision( -1 );
    name.setOrigin( origin );
    row.addValueMeta( name );

    // the object type
    //
    ValueMetaInterface objectType = new ValueMetaString( "object_type" );
    objectType.setLength( 500 );
    objectType.setPrecision( -1 );
    objectType.setOrigin( origin );
    row.addValueMeta( objectType );

    // object_id
    //
    ValueMetaInterface objectId = new ValueMetaString( "object_id" );
    object.setLength( 500 );
    object.setPrecision( -1 );
    objectId.setOrigin( origin );
    row.addValueMeta( objectId );

    // modified by
    //
    ValueMetaInterface modifiedBy = new ValueMetaString( "modified_by" );
    object.setLength( 500 );
    object.setPrecision( -1 );
    modifiedBy.setOrigin( origin );
    row.addValueMeta( modifiedBy );

    // modified date
    //
    ValueMetaInterface modifiedDate = new ValueMetaDate( "modified_date" );
    modifiedDate.setOrigin( origin );
    row.addValueMeta( modifiedDate );

    // description
    //
    ValueMetaInterface description = new ValueMetaString( "description" );
    description.setLength( 500 );
    description.setPrecision( -1 );
    description.setOrigin( origin );
    row.addValueMeta( description );

    if ( includeRowNumber ) {
      ValueMetaInterface v = new ValueMetaInteger( space.environmentSubstitute( rowNumberField ) );
      v.setLength( ValueMetaInterface.DEFAULT_INTEGER_LENGTH, 0 );
      v.setOrigin( origin );
      row.addValueMeta( v );
    }

  }

  public String getXML() {
    StringBuilder retval = new StringBuilder( 300 );

    retval.append( "    " ).append( XMLHandler.addTagValue( "object_type", objectTypeSelection.toString() ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "rownum", includeRowNumber ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "rownum_field", rowNumberField ) );

    retval.append( "    <file>" ).append( Const.CR );

    for ( int i = 0; i < directory.length; i++ ) {
      retval.append( "      " ).append( XMLHandler.addTagValue( "directory", directory[i] ) );
      retval.append( "      " ).append( XMLHandler.addTagValue( "name_mask", nameMask[i] ) );
      retval.append( "      " ).append( XMLHandler.addTagValue( "exclude_name_mask", excludeNameMask[i] ) );
      retval.append( "      " ).append( XMLHandler.addTagValue( "include_subfolders", includeSubFolders[i] ) );
    }
    retval.append( "    </file>" ).append( Const.CR );

    return retval.toString();
  }

  public void loadXML( Node stepnode, List<DatabaseMeta> databases, IMetaStore metaStore ) throws HopXMLException {
    try {
      String objectTypeString = XMLHandler.getTagValue( stepnode, "object_type" );
      if ( objectTypeString != null ) {
        objectTypeSelection = ObjectTypeSelection.valueOf( objectTypeString );
      }
      if ( objectTypeSelection == null ) {
        objectTypeSelection = ObjectTypeSelection.All;
      }
      includeRowNumber = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "rownum" ) );
      rowNumberField = XMLHandler.getTagValue( stepnode, "rownum_field" );

      Node filenode = XMLHandler.getSubNode( stepnode, "file" );
      int nrfiles = XMLHandler.countNodes( filenode, "directory" );

      allocate( nrfiles );

      for ( int i = 0; i < nrfiles; i++ ) {
        Node filenamenode = XMLHandler.getSubNodeByNr( filenode, "directory", i );
        Node filemasknode = XMLHandler.getSubNodeByNr( filenode, "name_mask", i );
        Node excludefilemasknode = XMLHandler.getSubNodeByNr( filenode, "exclude_name_mask", i );
        Node includeSubFoldersnode = XMLHandler.getSubNodeByNr( filenode, "include_subfolders", i );
        directory[i] = XMLHandler.getNodeValue( filenamenode );
        nameMask[i] = XMLHandler.getNodeValue( filemasknode );
        excludeNameMask[i] = XMLHandler.getNodeValue( excludefilemasknode );
        includeSubFolders[i] = "Y".equalsIgnoreCase( XMLHandler.getNodeValue( includeSubFoldersnode ) );
      }
    } catch ( Exception e ) {
      throw new HopXMLException( "Unable to load step info from XML", e );
    }
  }

  public void readRep( Repository rep, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases ) throws HopException {
    try {
      int nrfiles = rep.countNrStepAttributes( id_step, "directory" );
      String objectTypeString = rep.getStepAttributeString( id_step, "object_type" );
      if ( objectTypeString != null ) {
        objectTypeSelection = ObjectTypeSelection.valueOf( objectTypeString );
      }
      if ( objectTypeSelection == null ) {
        objectTypeSelection = ObjectTypeSelection.All;
      }
      includeRowNumber = rep.getStepAttributeBoolean( id_step, "rownum" );
      rowNumberField = rep.getStepAttributeString( id_step, "rownum_field" );

      allocate( nrfiles );

      for ( int i = 0; i < nrfiles; i++ ) {
        directory[i] = rep.getStepAttributeString( id_step, i, "directory" );
        nameMask[i] = rep.getStepAttributeString( id_step, i, "name_mask" );
        excludeNameMask[i] = rep.getStepAttributeString( id_step, i, "exclude_name_mask" );
        includeSubFolders[i] = rep.getStepAttributeBoolean( id_step, i, "include_subfolders" );
      }
    } catch ( Exception e ) {
      throw new HopException( "Unexpected error reading step information from the repository", e );
    }
  }

  public void saveRep( Repository rep, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step ) throws HopException {
    try {
      rep.saveStepAttribute( id_transformation, id_step, "object_type", objectTypeSelection.toString() );
      rep.saveStepAttribute( id_transformation, id_step, "rownum", includeRowNumber );
      rep.saveStepAttribute( id_transformation, id_step, "rownum_field", rowNumberField );

      for ( int i = 0; i < directory.length; i++ ) {
        rep.saveStepAttribute( id_transformation, id_step, i, "directory", directory[i] );
        rep.saveStepAttribute( id_transformation, id_step, i, "name_mask", nameMask[i] );
        rep.saveStepAttribute( id_transformation, id_step, i, "exclude_name_mask", excludeNameMask[i] );
        rep.saveStepAttribute( id_transformation, id_step, i, "include_subfolders", includeSubFolders[i] );
      }
    } catch ( Exception e ) {
      throw new HopException( "Unable to save step information to the repository for id_step=" + id_step, e );
    }
  }

  public boolean[] getIncludeSubFolders() {
    return includeSubFolders;
  }

  public void check( List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepMeta,
    RowMetaInterface prev, String[] input, String[] output, RowMetaInterface info, VariableSpace space,
    Repository repository, IMetaStore metaStore ) {
    CheckResult cr;

    if ( input.length > 0 ) {
      cr =
        new CheckResult( CheckResultInterface.TYPE_RESULT_ERROR, BaseMessages.getString(
          PKG, "GetRepositoryNamesMeta.CheckResult.NoInputError" ), stepMeta );
    } else {
      cr =
        new CheckResult( CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(
          PKG, "GetRepositoryNamesMeta.CheckResult.NoInputOk" ), stepMeta );
    }
    remarks.add( cr );
  }

  public StepInterface getStep( StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr,
    TransMeta transMeta, Trans trans ) {
    return new GetRepositoryNames( stepMeta, stepDataInterface, cnr, transMeta, trans );
  }

  public StepDataInterface getStepData() {
    return new GetRepositoryNamesData();
  }

  /**
   * @return the includeRowNumber
   */
  public boolean isIncludeRowNumber() {
    return includeRowNumber;
  }

  /**
   * @param includeRowNumber
   *          the includeRowNumber to set
   */
  public void setIncludeRowNumber( boolean includeRowNumber ) {
    this.includeRowNumber = includeRowNumber;
  }

  /**
   * @param includeSubFolders
   *          the includeSubFolders to set
   */
  public void setIncludeSubFolders( boolean[] includeSubFolders ) {
    this.includeSubFolders = includeSubFolders;
  }

  /**
   * @param rowNumberField
   *          the rowNumberField to set
   */
  public void setRowNumberField( String rowNumberField ) {
    this.rowNumberField = rowNumberField;
  }

  /**
   * @return the objectTypeSelection
   */
  public ObjectTypeSelection getObjectTypeSelection() {
    return objectTypeSelection;
  }

  /**
   * @param objectTypeSelection
   *          the objectTypeSelection to set
   */
  public void setObjectTypeSelection( ObjectTypeSelection objectTypeSelection ) {
    this.objectTypeSelection = objectTypeSelection;
  }

  /**
   * @return the directory
   */
  public String[] getDirectory() {
    return directory;
  }

  /**
   * @param directory
   *          the directory to set
   */
  public void setDirectory( String[] directory ) {
    this.directory = directory;
  }

  /**
   * @return the nameMask
   */
  public String[] getNameMask() {
    return nameMask;
  }

  /**
   * @param nameMask
   *          the nameMask to set
   */
  public void setNameMask( String[] nameMask ) {
    this.nameMask = nameMask;
  }

  /**
   * @return the excludeNameMask
   */
  public String[] getExcludeNameMask() {
    return excludeNameMask;
  }

  /**
   * @param excludeNameMask
   *          the excludeNameMask to set
   */
  public void setExcludeNameMask( String[] excludeNameMask ) {
    this.excludeNameMask = excludeNameMask;
  }
}
