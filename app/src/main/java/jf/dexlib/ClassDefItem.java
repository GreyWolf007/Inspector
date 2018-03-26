/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver (JesusFreke)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jf.dexlib;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import jf.dexlib.EncodedValue.ArrayEncodedSubValue;
import jf.dexlib.EncodedValue.EncodedValue;
import jf.dexlib.Util.AccessFlags;
import jf.dexlib.Util.AnnotatedOutput;
import jf.dexlib.Util.Input;
import jf.dexlib.Util.TypeUtils;

public class ClassDefItem extends Item<ClassDefItem> {
    private TypeIdItem classType;
    public int accessFlags;
    public TypeIdItem superType;
    public TypeListItem implementedInterfaces;
    public StringIdItem sourceFile;
    private AnnotationDirectoryItem annotations;
    private ClassDataItem classData;
    private EncodedArrayItem staticFieldInitializers;

    /**
     * Creates a new uninitialized <code>ClassDefItem</code>
     * @param dexFile The <code>DexFile</code> that this item belongs to
     */
    protected ClassDefItem(DexFile dexFile) {
        super(dexFile);
    }

    /**
     * Creates a new <code>ClassDefItem</code> with the given values
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param classType The type of this class
     * @param accessFlags The access flags of this class
     * @param superType The superclass of this class, or null if none (only valid for java.lang.Object)
     * @param implementedInterfaces A list of the interfaces that this class implements, or null if none
     * @param sourceFile The main source file that this class is defined in, or null if not available
     * @param annotations The annotations for this class and its fields, methods and method parameters, or null if none
     * @param classData The <code>ClassDataItem</code> containing the method and field definitions for this class
     * @param staticFieldInitializers The initial values for this class's static fields, or null if none. The initial
     * values should be in the same order as the static fields in the <code>ClassDataItem</code>. It can contain
     * fewer items than static fields, in which case the remaining static fields will be initialized with a default
     * value of null/0. The initial value for any fields that don't specifically have a value can be either the
     * type-appropriate null/0 encoded value, or null.
     */
    private ClassDefItem(DexFile dexFile, TypeIdItem classType, int accessFlags, TypeIdItem superType,
                         TypeListItem implementedInterfaces, StringIdItem sourceFile,
                         AnnotationDirectoryItem annotations, ClassDataItem classData,
                         EncodedArrayItem staticFieldInitializers) {
        super(dexFile);
        assert classType != null;
        this.classType = classType;
        this.accessFlags = accessFlags;
        this.superType = superType;
        this.implementedInterfaces = implementedInterfaces;
        this.sourceFile = sourceFile;
        this.annotations = annotations;
        this.classData = classData;
        this.staticFieldInitializers = staticFieldInitializers;

        if (classData != null) {
            classData.setParent(this);
        }
        if (annotations != null) {
            annotations.setParent(this);
        }
    }

    /**
     * Returns a <code>ClassDefItem</code> for the given values, and that has been interned into the given
     * <code>DexFile</code>
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param classType The type of this class
     * @param accessFlags The access flags of this class
     * @param superType The superclass of this class, or null if none (only valid for java.lang.Object)
     * @param implementedInterfaces A list of the interfaces that this class implements, or null if none
     * @param sourceFile The main source file that this class is defined in, or null if not available
     * @param annotations The annotations for this class and its fields, methods and method parameters, or null if none
     * @param classData The <code>ClassDataItem</code> containing the method and field definitions for this class
     * @param staticFieldInitializers The initial values for this class's static fields, or null if none. If it is not
     * null, it must contain the same number of items as the number of static fields in this class. The value in the
     * <code>StaticFieldInitializer</code> for any field that doesn't have an explicit initial value can either be null
     * or be the type-appropriate null/0 value.
     * @return a <code>ClassDefItem</code> for the given values, and that has been interned into the given
     * <code>DexFile</code>
     */
    public static ClassDefItem internClassDefItem(DexFile dexFile, TypeIdItem classType, int accessFlags,
                         TypeIdItem superType, TypeListItem implementedInterfaces, StringIdItem sourceFile,
                         AnnotationDirectoryItem annotations, ClassDataItem classData,
                         List<StaticFieldInitializer> staticFieldInitializers) {
        EncodedArrayItem encodedArrayItem = null;
        if(!dexFile.getInplace() && staticFieldInitializers != null && staticFieldInitializers.size() > 0) {
            assert classData != null;
            assert staticFieldInitializers.size() == classData.getStaticFields().length;
            encodedArrayItem = makeStaticFieldInitializersItem(dexFile, staticFieldInitializers);
        }

        ClassDefItem classDefItem = new ClassDefItem(dexFile, classType, accessFlags, superType, implementedInterfaces,
                sourceFile, annotations, classData, encodedArrayItem);
        return dexFile.ClassDefsSection.intern(classDefItem);
    }

    
    public ClassDefItem internClassDefItem(DexFile dexFile) {
        ClassDefItem classDefItem = new ClassDefItem(
                dexFile,
                TypeIdItem.internTypeIdItem(
                    dexFile,
                    classType.getTypeDescriptor()),
                accessFlags,
                TypeIdItem.internTypeIdItem(
                    dexFile,
                    superType.getTypeDescriptor()),
                implementedInterfaces !=null ?implementedInterfaces.internTypeListItem(dexFile):null,

                sourceFile !=null?StringIdItem.internStringIdItem(
                    dexFile,
                    sourceFile.getStringValue()) : null,
                annotations!=null?annotations.copyAnnotationDirectoryItem(dexFile):null,
                classData != null ?classData.internClassDataItem(dexFile) : null,
                copyStaticFieldInit(dexFile,staticFieldInitializers)
                );
        return dexFile.ClassDefsSection.intern(classDefItem);
    }


    /**
     * Looks up a <code>ClassDefItem</code> from the given <code>DexFile</code> for the given
     * values
     * @param dexFile The <code>DexFile</code> that the <code>ClassDefItem</code> belongs to
     * @param classType The type of the class
     * @param accessFlags The access flags of the class
     * @param superType The superclass of the class, or null if none (only valid for java.lang.Object)
     * @param implementedInterfaces A list of the interfaces that the class implements, or null if none
     * @param sourceFile The main source file that the class is defined in, or null if not available
     * @param annotations The annotations for the class and its fields, methods and method parameters, or null if none
     * @param classData The <code>ClassDataItem</code> containing the method and field definitions for the class
     * @param staticFieldInitializers The initial values for the class's static fields, or null if none. If it is not
     * null, it must contain the same number of items as the number of static fields in the class. The value in the
     * <code>StaticFieldInitializer</code> for any field that doesn't have an explicit initial value can either be null
     * or be the type-appropriate null/0 value.
     * @return a <code>ClassDefItem</code> from the given <code>DexFile</code> for the given
     * values, or null if it doesn't exist
     */
    public static ClassDefItem lookupClassDefItem(DexFile dexFile, TypeIdItem classType, int accessFlags,
                         TypeIdItem superType, TypeListItem implementedInterfaces, StringIdItem sourceFile,
                         AnnotationDirectoryItem annotations, ClassDataItem classData,
                         List<StaticFieldInitializer> staticFieldInitializers) {
        EncodedArrayItem encodedArrayItem = null;
        if(!dexFile.getInplace() && staticFieldInitializers != null && staticFieldInitializers.size() > 0) {
            assert classData != null;
            assert staticFieldInitializers.size() == classData.getStaticFields().length;
            encodedArrayItem = makeStaticFieldInitializersItem(dexFile, staticFieldInitializers);
        }

        ClassDefItem classDefItem = new ClassDefItem(dexFile, classType, accessFlags, superType, implementedInterfaces,
                sourceFile, annotations, classData, encodedArrayItem);
        return dexFile.ClassDefsSection.getInternedItem(classDefItem);
    }

    /** {@inheritDoc} */
    protected void readItem(Input in, ReadContext readContext) {
        classType = dexFile.TypeIdsSection.getItemByIndex(in.readInt());
        accessFlags = in.readInt();
        superType = dexFile.TypeIdsSection.getOptionalItemByIndex(in.readInt());
        implementedInterfaces = (TypeListItem)readContext.getOptionalOffsettedItemByOffset(ItemType.TYPE_TYPE_LIST,
                in.readInt());
        sourceFile = dexFile.StringIdsSection.getOptionalItemByIndex(in.readInt());
        annotations = (AnnotationDirectoryItem)readContext.getOptionalOffsettedItemByOffset(
                ItemType.TYPE_ANNOTATIONS_DIRECTORY_ITEM, in.readInt());
        classData = (ClassDataItem)readContext.getOptionalOffsettedItemByOffset(ItemType.TYPE_CLASS_DATA_ITEM, in.readInt());
        staticFieldInitializers = (EncodedArrayItem)readContext.getOptionalOffsettedItemByOffset(
                ItemType.TYPE_ENCODED_ARRAY_ITEM, in.readInt());

        if (classData != null) {
            classData.setParent(this);
        }
        if (annotations != null) {
            annotations.setParent(this);
        }
    }

    /** {@inheritDoc} */
    protected int placeItem(int offset) {
        return offset + 32;
    }

    /** {@inheritDoc} */
    protected void writeItem(AnnotatedOutput out) {
        if (out.annotates()) {
            out.annotate(4, "class_type: " + classType.getTypeDescriptor());
            out.annotate(4, "access_flags: " + AccessFlags.formatAccessFlagsForClass(accessFlags));
            out.annotate(4, "superclass_type: " + (superType==null?"":superType.getTypeDescriptor()));
            out.annotate(4, "interfaces: " +
                    (implementedInterfaces==null?"":implementedInterfaces.getTypeListString(" ")));
            out.annotate(4, "source_file: " + (sourceFile==null?"":sourceFile.getStringValue()));
            out.annotate(4, "annotations_off: " +
                    (annotations==null?"":"0x"+Integer.toHexString(annotations.getOffset())));
            out.annotate(4, "class_data_off:" +
                    (classData==null?"":"0x"+Integer.toHexString(classData.getOffset())));
            out.annotate(4, "static_values_off: " +
                    (staticFieldInitializers==null?"":"0x"+Integer.toHexString(staticFieldInitializers.getOffset())));
        }
        out.writeInt(classType.getIndex());
        out.writeInt(accessFlags);
        out.writeInt(superType==null?-1:superType.getIndex());
        out.writeInt(implementedInterfaces==null?0:implementedInterfaces.getOffset());
        out.writeInt(sourceFile==null?-1:sourceFile.getIndex());
        out.writeInt(annotations==null?0:annotations.getOffset());
        out.writeInt(classData==null?0:classData.getOffset());
        out.writeInt(staticFieldInitializers==null?0:staticFieldInitializers.getOffset());
    }

    /** {@inheritDoc} */
    public ItemType getItemType() {
        return ItemType.TYPE_CLASS_DEF_ITEM;
    }

    /** {@inheritDoc} */
    public String getConciseIdentity() {
        return "class_def_item: " + classType.getTypeDescriptor();
    }

    /** {@inheritDoc} */
    public int compareTo(ClassDefItem o) {
        //The actual sorting for this class is implemented in SortClassDefItemSection.
        //This method is just used for sorting the associated ClassDataItem items, so
        //we can just do the comparison based on the offsets of the items
        return this.getOffset() - o.getOffset();
    }

    public TypeIdItem getClassType() {
        return classType;
    }

    public int getAccessFlags() {
        return accessFlags;
    }

    public TypeIdItem getSuperclass() {
        return superType;
    }

    public TypeListItem getInterfaces() {
        return implementedInterfaces;
    }

    public StringIdItem getSourceFile() {
        return sourceFile;
    }

    public AnnotationDirectoryItem getAnnotations() {
        return annotations;
    }

    public ClassDataItem getClassData() {
        return classData;
    }

    public EncodedArrayItem getStaticFieldInitializers() {
        return staticFieldInitializers;
    }



    private static EncodedArrayItem copyStaticFieldInit(DexFile dexFile,EncodedArrayItem staticFieldInitializers){
        if(staticFieldInitializers ==null){
            return null;
        }

        EncodedValue[] values=staticFieldInitializers.getEncodedArray().values;
        EncodedValue[] outValues=new EncodedValue[values.length];
        for(int i=0,len=values.length;i<len;i++){
            outValues[i]=EncodedValue.copyEncodedValue(dexFile,values[i]);
        }
        return EncodedArrayItem.internEncodedArrayItem(
                dexFile,
                new ArrayEncodedSubValue(outValues)
                );
    }



    public static int placeClassDefItems(IndexedSection<ClassDefItem> section, int offset) {
        ClassDefPlacer cdp = new ClassDefPlacer(section);
        return cdp.placeSection(offset);
    }

    /**
     * This class places the items within a ClassDefItem section, such that superclasses and interfaces are
     * placed before sub/implementing classes
     */
    private static class ClassDefPlacer {
        private final IndexedSection<ClassDefItem> section;
        private final HashMap<TypeIdItem, ClassDefItem> unplacedClassDefsByType =
                new HashMap<TypeIdItem, ClassDefItem>();

        private int currentIndex = 0;
        private int currentOffset;

        public ClassDefPlacer(IndexedSection<ClassDefItem> section) {
            this.section = section;

            for (ClassDefItem classDefItem: section.items) {
                TypeIdItem typeIdItem = classDefItem.classType;
                unplacedClassDefsByType.put(typeIdItem, classDefItem);
            }
        }

        public int placeSection(int offset) {
            currentOffset = offset;

            if (section.DexFile.getSortAllItems()) {
                //presort the list, to guarantee a unique ordering
                Collections.sort(section.items, new Comparator<ClassDefItem>() {
                    public int compare(ClassDefItem a, ClassDefItem b) {
                        return a.getClassType().compareTo(b.getClassType());
                    }
                });
            }

            //we need to initialize the offset for all the classes to -1, so we can tell which ones
            //have been placed
            for (ClassDefItem classDefItem: section.items) {
                classDefItem.offset = -1;
            }

            for (ClassDefItem classDefItem: section.items) {
                placeClass(classDefItem);
            }

            for (ClassDefItem classDefItem: unplacedClassDefsByType.values()) {
                section.items.set(classDefItem.getIndex(), classDefItem);
            }

            return currentOffset;
        }

        private void placeClass(ClassDefItem classDefItem) {
            if (classDefItem.getOffset() == -1) {
                TypeIdItem superType = classDefItem.superType;
                ClassDefItem superClassDefItem = unplacedClassDefsByType.get(superType);

                if (superClassDefItem != null) {
                    placeClass(superClassDefItem);
                }

                TypeListItem interfaces = classDefItem.implementedInterfaces;

                if (interfaces != null) {
                    for (TypeIdItem interfaceType: interfaces.getTypes()) {
                        ClassDefItem interfaceClass = unplacedClassDefsByType.get(interfaceType);
                        if (interfaceClass != null) {
                            placeClass(interfaceClass);
                        }
                    }
                }

                currentOffset = classDefItem.placeAt(currentOffset, currentIndex++);
                unplacedClassDefsByType.remove(classDefItem.classType);
            }
        }
    }

    public static class StaticFieldInitializer implements Comparable<StaticFieldInitializer> {
        public final EncodedValue value;
        public final ClassDataItem.EncodedField field;
        public StaticFieldInitializer(EncodedValue value, ClassDataItem.EncodedField field) {
            this.value = value;
            this.field = field;
        }

        public int compareTo(StaticFieldInitializer other) {
            return field.compareTo(other.field);
        }
    }


    /**
     * A helper method to sort the static field initializers and populate the default values as needed
     * @param dexFile the <code>DexFile</code>
     * @param staticFieldInitializers the initial values
     * @return an interned EncodedArrayItem containing the static field initializers
     */
    private static EncodedArrayItem makeStaticFieldInitializersItem(DexFile dexFile,
                                                               List<StaticFieldInitializer> staticFieldInitializers) {
        if (staticFieldInitializers == null || staticFieldInitializers.size() == 0) {
            return null;
        }

        int len = staticFieldInitializers.size();

        Collections.sort(staticFieldInitializers);

        int lastIndex = -1;
        for (int i=len-1; i>=0; i--) {
            StaticFieldInitializer staticFieldInitializer = staticFieldInitializers.get(i);

            if (staticFieldInitializer.value != null &&
                    (staticFieldInitializer.value.compareTo(TypeUtils.makeDefaultValueForType(dexFile,
                    staticFieldInitializer.field.field.getFieldType().getTypeDescriptor())) != 0)) {
                lastIndex = i;
                break;
            }
        }

        //we don't have any non-null/non-default values, so we don't need to create an EncodedArrayItem
        if (lastIndex == -1) {
            return null;
        }

        EncodedValue[] values = new EncodedValue[lastIndex+1];

        for (int i=0; i<=lastIndex; i++) {
            StaticFieldInitializer staticFieldInitializer = staticFieldInitializers.get(i);
            EncodedValue encodedValue = staticFieldInitializer.value;
            if (encodedValue == null) {
                encodedValue = TypeUtils.makeDefaultValueForType(dexFile,
                        staticFieldInitializer.field.field.getFieldType().getTypeDescriptor());
            }

            values[i] = encodedValue;
        }

        ArrayEncodedSubValue encodedArrayValue = new ArrayEncodedSubValue(values);
        return EncodedArrayItem.internEncodedArrayItem(dexFile, encodedArrayValue);
    }
}
