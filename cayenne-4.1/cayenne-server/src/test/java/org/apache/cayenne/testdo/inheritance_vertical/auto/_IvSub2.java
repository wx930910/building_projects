package org.apache.cayenne.testdo.inheritance_vertical.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.cayenne.exp.Property;
import org.apache.cayenne.testdo.inheritance_vertical.IvRoot;

/**
 * Class _IvSub2 was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _IvSub2 extends IvRoot {

    private static final long serialVersionUID = 1L; 

    public static final String ID_PK_COLUMN = "ID";

    public static final Property<String> SUB2ATTR = Property.create("sub2Attr", String.class);
    public static final Property<String> SUB2NAME = Property.create("sub2Name", String.class);

    protected String sub2Attr;
    protected String sub2Name;


    public void setSub2Attr(String sub2Attr) {
        beforePropertyWrite("sub2Attr", this.sub2Attr, sub2Attr);
        this.sub2Attr = sub2Attr;
    }

    public String getSub2Attr() {
        beforePropertyRead("sub2Attr");
        return this.sub2Attr;
    }

    public void setSub2Name(String sub2Name) {
        beforePropertyWrite("sub2Name", this.sub2Name, sub2Name);
        this.sub2Name = sub2Name;
    }

    public String getSub2Name() {
        beforePropertyRead("sub2Name");
        return this.sub2Name;
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "sub2Attr":
                return this.sub2Attr;
            case "sub2Name":
                return this.sub2Name;
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
            case "sub2Attr":
                this.sub2Attr = (String)val;
                break;
            case "sub2Name":
                this.sub2Name = (String)val;
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
        out.writeObject(this.sub2Attr);
        out.writeObject(this.sub2Name);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.sub2Attr = (String)in.readObject();
        this.sub2Name = (String)in.readObject();
    }

}