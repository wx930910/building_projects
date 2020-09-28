package org.apache.cayenne.testdo.locking.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.cayenne.BaseDataObject;
import org.apache.cayenne.exp.Property;

/**
 * Class _SimpleLockingTestEntity was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _SimpleLockingTestEntity extends BaseDataObject {

    private static final long serialVersionUID = 1L; 

    public static final String LOCKING_TEST_ID_PK_COLUMN = "LOCKING_TEST_ID";

    public static final Property<String> DESCRIPTION = Property.create("description", String.class);
    public static final Property<Integer> INT_COLUMN_NOTNULL = Property.create("intColumnNotnull", Integer.class);
    public static final Property<Integer> INT_COLUMN_NULL = Property.create("intColumnNull", Integer.class);
    public static final Property<String> NAME = Property.create("name", String.class);

    protected String description;
    protected int intColumnNotnull;
    protected Integer intColumnNull;
    protected String name;


    public void setDescription(String description) {
        beforePropertyWrite("description", this.description, description);
        this.description = description;
    }

    public String getDescription() {
        beforePropertyRead("description");
        return this.description;
    }

    public void setIntColumnNotnull(int intColumnNotnull) {
        beforePropertyWrite("intColumnNotnull", this.intColumnNotnull, intColumnNotnull);
        this.intColumnNotnull = intColumnNotnull;
    }

    public int getIntColumnNotnull() {
        beforePropertyRead("intColumnNotnull");
        return this.intColumnNotnull;
    }

    public void setIntColumnNull(int intColumnNull) {
        beforePropertyWrite("intColumnNull", this.intColumnNull, intColumnNull);
        this.intColumnNull = intColumnNull;
    }

    public int getIntColumnNull() {
        beforePropertyRead("intColumnNull");
        if(this.intColumnNull == null) {
            return 0;
        }
        return this.intColumnNull;
    }

    public void setName(String name) {
        beforePropertyWrite("name", this.name, name);
        this.name = name;
    }

    public String getName() {
        beforePropertyRead("name");
        return this.name;
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "description":
                return this.description;
            case "intColumnNotnull":
                return this.intColumnNotnull;
            case "intColumnNull":
                return this.intColumnNull;
            case "name":
                return this.name;
            default:
                return super.readPropertyDirectly(propName);
        }
    }

    @Override
    public void writePropertyDirectly(String propName, Object val) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch (propName) {
            case "description":
                this.description = (String)val;
                break;
            case "intColumnNotnull":
                this.intColumnNotnull = val == null ? 0 : (int)val;
                break;
            case "intColumnNull":
                this.intColumnNull = (Integer)val;
                break;
            case "name":
                this.name = (String)val;
                break;
            default:
                super.writePropertyDirectly(propName, val);
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        writeSerialized(out);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        readSerialized(in);
    }

    @Override
    protected void writeState(ObjectOutputStream out) throws IOException {
        super.writeState(out);
        out.writeObject(this.description);
        out.writeInt(this.intColumnNotnull);
        out.writeObject(this.intColumnNull);
        out.writeObject(this.name);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.description = (String)in.readObject();
        this.intColumnNotnull = in.readInt();
        this.intColumnNull = (Integer)in.readObject();
        this.name = (String)in.readObject();
    }

}