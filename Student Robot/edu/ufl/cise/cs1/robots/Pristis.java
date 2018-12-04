package edu.ufl.cise.cs1.robots;

import robocode.ScannedRobotEvent;
import robocode.TeamRobot;
import robocode.util.Utils;

/**
 * "Pristis" - a robot by Thiago de Andrade
 *
 * This robot uses a very simple implementation of the stop-and-go strategy
 * to dodge bullets.
 *
 * The targeting algorithm uses linear targeting (based on trigonometry)
 * in order to "predict" the position of target when the bullet has
 * travelled long enough to reach it. It is not optimized, but works much
 * better than just shooting straight.
 *
 * All strategies used are found in the robocode wiki and completely implemented
 * by the author, except when noted.
 *
 */

public class Pristis extends TeamRobot {

    private double previousTargetEnergy = 100; //keeps track of target's energy
    private boolean lastMove = false; //used to reverse direction

    public void run() {
        //FIXME: program to go close to center to patrol, if target isn't found

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        //Adjusts Gun and Radar to Robot's body turn

        while (true) {
            turnRadarLeft(Double.POSITIVE_INFINITY);
            //constantly rotates radar and patrols until finds target
        }
    }

    public void onScannedRobot(ScannedRobotEvent target) {
        //FIXME program to approach very distant targets
        if (isTeammate(target.getName())) {
            return; //if target is a teammate, return to run() behavior and look for next target
        }
        double absBearing = getHeadingRadians() + target.getBearingRadians();
        setTurnRightRadians(Utils.normalRelativeAngle(target.getBearingRadians() - Math.PI/2));
        //keeps a 90 degree angle relative to the target, making dodging more efficient
        double firePower = fireManagement(target);
        double gunTurnAngle;
        double radarTurnAngle;
        double predictedAngleDifference = targetSystem(target, absBearing);
        gunTurnAngle = absBearing - getGunHeadingRadians() + predictedAngleDifference;
        radarTurnAngle = absBearing - getRadarHeadingRadians();
        setTurnRadarRightRadians(Utils.normalRelativeAngle(radarTurnAngle));
        setTurnGunRightRadians(Utils.normalRelativeAngle(gunTurnAngle));
        //locking gun on target
        fire(firePower);
        stopAndGo(target); //move if enemy has shot last turn
    }

    private void stopAndGo(ScannedRobotEvent target) {
        //FIXME: program to avoid walls while stopping and going.
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

    private double targetSystem(ScannedRobotEvent target, double absBearing) {
        return (target.getVelocity() * Math.sin(target.getHeadingRadians() - absBearing) / 13.0); //"predicts" future position
        // based on target current velocity and bearing. FIXME ADD REFERENCE and explanation
    }

    private double fireManagement(ScannedRobotEvent target) {
        double firePower;
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
        return firePower;
    }
}
