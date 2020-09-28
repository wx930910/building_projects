package org.apache.cayenne.testdo.inheritance_vertical.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.cayenne.exp.Property;
import org.apache.cayenne.testdo.inheritance_vertical.IvBaseWithLock;
import org.apache.cayenne.testdo.inheritance_vertical.IvOther;

/**
 * Class _IvImplWithLock was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _IvImplWithLock extends IvBaseWithLock {

    private static final long serialVersionUID = 1L; 

    public static final String ID_PK_COLUMN = "ID";

    public static final Property<String> ATTR1 = Property.create("attr1", String.class);
    public static final Property<IvOther> OTHER1 = Property.create("other1", IvOther.class);

    protected String attr1;

    protected Object other1;

    public void setAttr1(String attr1) {
        beforePropertyWrite("attr1", this.attr1, attr1);
        this.attr1 = attr1;
    }

    public String getAttr1() {
        beforePropertyRead("attr1");
        return this.attr1;
    }

    public void setOther1(IvOther other1) {
        setToOneTarget("other1", other1, true);
    }

    public IvOther getOther1() {
        return (IvOther)readProperty("other1");
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "attr1":
                return this.attr1;
            case "other1":
                return this.other1;
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
            case "attr1":
                this.attr1 = (String)val;
                break;
            case "other1":
                this.other1 = val;
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
        out.writeObject(this.attr1);
        out.writeObject(this.other1);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.attr1 = (String)in.readObject();
        this.other1 = in.readObject();
    }

}
