package gregtech.common.covers.redstone;

import gregtech.api.GregTech_API;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.ICoverable;

public class GT_Cover_AdvancedRedstoneTransmitterExternal extends GT_Cover_AdvancedRedstoneTransmitterBase {

    public GT_Cover_AdvancedRedstoneTransmitterExternal(ITexture coverTexture) {
        super(coverTexture);
    }

    @Override
    public TransmitterData doCoverThingsImpl(byte aSide, byte aInputRedstone, int aCoverID,
                                             TransmitterData aCoverVariable, ICoverable aTileEntity, long aTimer) {
        byte outputRedstone = aInputRedstone;
        if (aCoverVariable.isInvert()) {
            if (outputRedstone > 0) outputRedstone = 0;
            else outputRedstone = 15;
        }

        long hash = GregTech_API.hashCoverCoords(aTileEntity, aSide);
        setSignalAt(aCoverVariable.getUuid(), aCoverVariable.getFrequency(), hash, outputRedstone);

        return aCoverVariable;
    }

    @Override
    public boolean letsRedstoneGoInImpl(byte aSide, int aCoverID, TransmitterData aCoverVariable, ICoverable aTileEntity) {
        return true;
    }
}
