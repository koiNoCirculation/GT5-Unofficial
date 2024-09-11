package gregtech.api.graphs.paths;

import net.minecraft.server.MinecraftServer;

import gregtech.api.enums.TickTime;
import gregtech.api.metatileentity.BaseMetaPipeEntity;
import gregtech.api.metatileentity.MetaPipeEntity;
import gregtech.api.metatileentity.implementations.GT_MetaPipeEntity_Cable;
import gregtech.api.util.AveragePerTickCounter;
import gregtech.api.util.GT_Log;
import gregtech.common.misc.RecipeTimeAdjuster;

// path for cables
// all calculations like amp and voltage happens here
public class PowerNodePath extends NodePath {

    static double lastMsptMultiplier = 1.0;

    long mMaxAmps;
    long mAmps = 0;
    long mLoss;
    long mVoltage = 0;
    long mMaxVoltage;
    int mTick = 0;
    boolean mCountUp = true;

    private AveragePerTickCounter avgAmperageCounter = new AveragePerTickCounter(TickTime.SECOND);
    private AveragePerTickCounter avgVoltageCounter = new AveragePerTickCounter(TickTime.SECOND);

    public PowerNodePath(MetaPipeEntity[] aCables) {
        super(aCables);
    }

    public long getLoss() {
        return mLoss;
    }

    public void applyVoltage(long aVoltage, boolean aCountUp) {
        double multiplierByMSPT = RecipeTimeAdjuster.getMultiplierByMSPT();
        avgVoltageCounter.addValue(Math.max(aVoltage - mLoss, 0));

        int tNewTime = MinecraftServer.getServer()
            .getTickCounter();
        if (mTick != tNewTime || multiplierByMSPT != lastMsptMultiplier) {
            reset(tNewTime - mTick);
            mTick = tNewTime;
            this.mVoltage = aVoltage;
            this.mCountUp = aCountUp;
        } else if (this.mCountUp != aCountUp && (aVoltage - mLoss) > this.mVoltage || aVoltage > this.mVoltage) {
            this.mCountUp = aCountUp;
            this.mVoltage = aVoltage;
        }
        if (aVoltage > mMaxVoltage) {
            lock.addTileEntity(null);
            for (MetaPipeEntity tCable : mPipes) {
                if (((GT_MetaPipeEntity_Cable) tCable).mVoltage < this.mVoltage) {
                    BaseMetaPipeEntity tBaseCable = (BaseMetaPipeEntity) tCable.getBaseMetaTileEntity();
                    if (tBaseCable != null) {
                        GT_Log.exp.println(
                            String.format(
                                "Cable at %d, %d, %d burned, maxVolt = %d, inputVolt = %d",
                                tBaseCable.xCoord,
                                tBaseCable.yCoord,
                                tBaseCable.zCoord,
                                mMaxVoltage,
                                aVoltage));
                        tBaseCable.setToFire();
                    }
                }
            }
        }
        lastMsptMultiplier = multiplierByMSPT;
    }

    private void reset(int aTimePassed) {
        double multiplierByMSPT = RecipeTimeAdjuster.getMultiplierByMSPT();
        // when multiplier change, reset mAmps to 0 to avoid accidental cable burn
        if (aTimePassed < 0 || aTimePassed > 100 || multiplierByMSPT != lastMsptMultiplier) {
            mAmps = 0;
            return;
        }
        long maxAmpAdjusted = (long) Math.ceil(mMaxAmps * multiplierByMSPT);
        mAmps = Math.max(0, mAmps - (maxAmpAdjusted * aTimePassed));
    }

    public void addAmps(long aAmps) {
        double multiplierByMSPT = RecipeTimeAdjuster.getMultiplierByMSPT();
        long maxAmpAdjusted = (long) Math.ceil(mMaxAmps * multiplierByMSPT);
        avgAmperageCounter.addValue(aAmps);

        this.mAmps += aAmps;
        if (this.mAmps > maxAmpAdjusted * 40) {
            lock.addTileEntity(null);
            for (MetaPipeEntity tCable : mPipes) {
                if (((GT_MetaPipeEntity_Cable) tCable).mAmperage * multiplierByMSPT * 40 < this.mAmps) {
                    BaseMetaPipeEntity tBaseCable = (BaseMetaPipeEntity) tCable.getBaseMetaTileEntity();
                    if (tBaseCable != null) {
                        tBaseCable.setToFire();
                        GT_Log.exp.println(
                            String.format(
                                "Cable at %d, %d, %d burned, maxAmp = %d, inputAmp = %d, cumulatedAmp = %d",
                                tBaseCable.xCoord,
                                tBaseCable.yCoord,
                                tBaseCable.zCoord,
                                maxAmpAdjusted,
                                aAmps,
                                mAmps));
                    }
                }
            }
        }
    }

    // if no amps pass through for more than 0.5 second reduce them to minimize wrong results
    // but still allow the player to see if activity is happening
    @Deprecated
    public long getAmps() {
        double multiplierByMSPT = RecipeTimeAdjuster.getMultiplierByMSPT();
        int tTime = MinecraftServer.getServer()
            .getTickCounter() - 10;
        if (mTick < tTime || multiplierByMSPT != lastMsptMultiplier) {
            reset(tTime - mTick);
            mTick = tTime;
        }
        lastMsptMultiplier = multiplierByMSPT;
        return mAmps;
    }

    @Deprecated
    public long getVoltage(MetaPipeEntity aCable) {
        int tLoss = 0;
        if (mCountUp) {
            for (MetaPipeEntity mPipe : mPipes) {
                GT_MetaPipeEntity_Cable tCable = (GT_MetaPipeEntity_Cable) mPipe;
                tLoss += tCable.mCableLossPerMeter;
                if (aCable == tCable) {
                    return Math.max(mVoltage - tLoss, 0);
                }
            }
        } else {
            for (int i = mPipes.length - 1; i >= 0; i--) {
                GT_MetaPipeEntity_Cable tCable = (GT_MetaPipeEntity_Cable) mPipes[i];
                tLoss += tCable.mCableLossPerMeter;
                if (aCable == tCable) {
                    return Math.max(mVoltage - tLoss, 0);
                }
            }
        }
        return -1;
    }

    public long getAmperage() {
        return avgAmperageCounter.getLast();
    }

    public double getAvgAmperage() {
        return avgAmperageCounter.getAverage();
    }

    public long getVoltage() {
        return avgVoltageCounter.getLast();
    }

    public double getAvgVoltage() {
        return avgVoltageCounter.getAverage();
    }

    @Override
    protected void processPipes() {
        super.processPipes();
        mMaxAmps = Integer.MAX_VALUE;
        mMaxVoltage = Integer.MAX_VALUE;
        for (MetaPipeEntity tCable : mPipes) {
            if (tCable instanceof GT_MetaPipeEntity_Cable) {
                mMaxAmps = Math.min((long) Math.ceil(((GT_MetaPipeEntity_Cable) tCable).mAmperage), mMaxAmps);
                mLoss += ((GT_MetaPipeEntity_Cable) tCable).mCableLossPerMeter;
                mMaxVoltage = Math.min(((GT_MetaPipeEntity_Cable) tCable).mVoltage, mMaxVoltage);
            }
        }
    }
}
