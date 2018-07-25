package Punisher.NaCl.Internal.Ed25519Ref10;

public class GroupElementPreComp
{
    public FieldElement yplusx;
    public FieldElement yminusx;
    public FieldElement xy2d;
    
    public GroupElementPreComp()
    {
        yplusx = new FieldElement();
        yminusx = new FieldElement();
        xy2d = new FieldElement();
    }            

    public GroupElementPreComp(FieldElement yplusx, FieldElement yminusx, FieldElement xy2d)
    {
        this.yplusx = yplusx;
        this.yminusx = yminusx;
        this.xy2d = xy2d;
    }
}