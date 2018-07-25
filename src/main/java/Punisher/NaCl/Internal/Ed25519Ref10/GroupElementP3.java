package Punisher.NaCl.Internal.Ed25519Ref10;

public class GroupElementP3
{
    public FieldElement X;
    public FieldElement Y;
    public FieldElement Z;
    public FieldElement T;
    
    public void ReInit()
    {
        X = new FieldElement();
        Y = new FieldElement();
        Z = new FieldElement();
        T = new FieldElement();
    }
    
    public GroupElementP3()
    {
        X = new FieldElement();
        Y = new FieldElement();
        Z = new FieldElement();
        T = new FieldElement();
    }
}