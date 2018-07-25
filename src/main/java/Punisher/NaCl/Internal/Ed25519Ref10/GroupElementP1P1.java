package Punisher.NaCl.Internal.Ed25519Ref10;

public class GroupElementP1P1
{
    public FieldElement X;
    public FieldElement Y;
    public FieldElement Z;
    public FieldElement T;
    
    public GroupElementP1P1()
    {
        X = new FieldElement();
        Y = new FieldElement();
        Z = new FieldElement();
        T = new FieldElement();
    }
}