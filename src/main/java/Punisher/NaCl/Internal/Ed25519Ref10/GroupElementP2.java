package Punisher.NaCl.Internal.Ed25519Ref10;

public class GroupElementP2
{
    public FieldElement X;
    public FieldElement Y;
    public FieldElement Z;
    
    public GroupElementP2()
    {
        X = new FieldElement();
        Y = new FieldElement();
        Z = new FieldElement();        
    }
}