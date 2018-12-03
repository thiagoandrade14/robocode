package edu.ufl.cise.cs1.robots;

import robocode.ScannedRobotEvent;
import robocode.TeamRobot;
import robocode.util.Utils;

public class Pristis extends TeamRobot {

    private double previousTargetEnergy = 100; //used to keep track of target's shooting
    private boolean lastMove = false; //used to interchange ahead and back movements

    public void run() {
        //possible optimization: go close to center to patrol, if target isn't found
        //Adjusts Gun and Radar to Robot's body turn
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);        
        //constantly rotates radar and patrols until finds target
        while (true) {
            turnRadarLeft(Double.POSITIVE_INFINITY);
        }
    }

    public void onScannedRobot(ScannedRobotEvent target) {
        //further optimization: avoid walls while stopping and going
        //approach very distant targets
        if (isTeammate(target.getName())) {
            return; //if target is a teammate, return to run() behavior
        }
        double relativeBearing = getHeadingRadians() + target.getBearingRadians();
        //keeps a 90 degree angle relative to the target, making dodging more efficient
        setTurnRightRadians(Utils.normalRelativeAngle(target.getBearingRadians() - Math.PI/2));
        double firePower;
        double gunTurnAngle;
        double radarTurnAngle; //used for locking radar on target (reference wiki tutorial)
        double predictedAngleDifference = (target.getVelocity() * Math.sin(target.getHeadingRadians()
                                    - relativeBearing) / 13.0); //"predicts" future position
                                    // based on target current velocity and bearing. FIXME ADD REFERENCE
        gunTurnAngle = relativeBearing - getGunHeadingRadians() + predictedAngleDifference;
        radarTurnAngle = relativeBearing - getRadarHeadingRadians();
        if (target.getEnergy() < 3.0) {
            firePower = target.getEnergy() + 0.1; //avoids wasting energy on dying or disabled targets.
        }
        //choose firePower based on distance to target
        else if (target.getDistance() < 175) {
            firePower = 3;
        }
        else if(target.getDistance() < 300) {
            firePower = 2;
        }
        else {
            firePower = 1;
        }
        setTurnRadarRightRadians(Utils.normalRelativeAngle(radarTurnAngle));
        setTurnGunRightRadians(Utils.normalRelativeAngle(gunTurnAngle));
        //locking gun on target
        fire(firePower);
        //stop and go strategy: if target's energy depleted by less than 3
        // (meaning it fired last turn), move ahead or back
        if (previousTargetEnergy <= target.getEnergy() + 3 && previousTargetEnergy > target.getEnergy()) {
            if (!lastMove) {
                setAhead(100);
                lastMove = true;
            }
            else {
                setBack(100);
                lastMove = false;
            }
        }
        previousTargetEnergy = target.getEnergy();
    }
}
