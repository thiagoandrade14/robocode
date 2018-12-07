package edu.ufl.cise.cs1.robots;

import robocode.HitRobotEvent;
import robocode.ScannedRobotEvent;
import robocode.TeamRobot;
import robocode.util.Utils;

/**
 * "Pristis" - a robot by Thiago de Andrade
 *
 * This robot uses a very simple implementation of the stop-and-go strategy
 * to dodge bullets.
 *
 * The targeting algorithm uses linear targeting based on trigonometry
 * in order to "predict" the position of target when the bullet has
 * travelled long enough to reach it. It is not optimized, but works much
 * better than just shooting straight.
 *
 * All ideas and strategies used were found in sample robots or in the robocode wiki.
 * They were completely implemented by the author, except when noted.
 *
 */

public class Pristis extends TeamRobot {

    private double previousTargetEnergy = 100; //keeps track of target's energy
    private boolean lastMove = false; //used to reverse direction
    private String enemyOfTheState = "nemesis";
    private boolean wasRammed = false;

    public void run() {
        setAdjustGunForRobotTurn(true); //Adjusts Gun and Radar to Robot's body turn
        setAdjustRadarForGunTurn(true);
        while (true) {
            getOutOfEdge();
            turnRadarLeftRadians(Double.POSITIVE_INFINITY);
            //constantly rotates radar and patrols until finds target
        }
    }

    public void onScannedRobot(ScannedRobotEvent target) {
        if (isTeammate(target.getName())) {
            return; //if target is a teammate, return to run() behavior and look for next target
        }
        double firePower = fireManagement(target);
        double absBearing = getHeadingRadians() + target.getBearingRadians();
        enemyOfTheState = target.getName();

        if (target.getEnergy() < 20) { //target is weak
            if (getEnergy() > 30) { //recklessly charge
                targetSystem(target, absBearing, firePower);
                fire(fireManagement(target));
                charge(absBearing);
            }
            else {
                if (target.getDistance() > 100) { //approach weak enemy to finish off with bullets
                    charge(absBearing);
                }
                targetSystem(target, absBearing, firePower);
                fire(fireManagement(target));
            }
        }
        else {
            setTurnRightRadians(Utils.normalRelativeAngle(target.getBearingRadians() - Math.PI / 2));
            //keeps a 90 degree angle relative to the target, making dodging more efficient
            targetSystem(target, absBearing, firePower);
            fire(firePower);
            stopAndGo(target); //move if enemy has shot last turn
        }
    }

    public void onHitRobot(HitRobotEvent event) {
        if (!event.getName().equals(enemyOfTheState) && !event.isMyFault() && !wasRammed) {
            if (event.getBearingRadians() < Math.PI/2 || event.getBearingRadians() > 3* Math.PI/2) {
                setBack(100);
            }
            else {
                setAhead(100);
            }
            out.println("Ouch!");
            turnRadarLeftRadians(event.getBearingRadians() + getRadarHeadingRadians());
            enemyOfTheState = event.getName();
            wasRammed = true;
        }
    }

    private void stopAndGo(ScannedRobotEvent target) {
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

    private void targetSystem(ScannedRobotEvent target, double absBearing, double firePower) {
        /*"predicts" future position based on target current velocity
         and bearing. Code was implemented by the author, but inspired by tutorial and formulas found at
         robowiki.net/wiki/Linear_targeting
        */
        double predictedChange = (target.getVelocity() * Math.sin(target.getHeadingRadians()
                                                       - absBearing) / (20 - (3*firePower)));
        double gunTurnAngle = absBearing - getGunHeadingRadians() + predictedChange;
        double radarTurnAngle = absBearing - getRadarHeadingRadians();
        setTurnRadarRightRadians(Utils.normalRelativeAngle(radarTurnAngle));
        setTurnGunRightRadians(Utils.normalRelativeAngle(gunTurnAngle));
    }

    private double fireManagement(ScannedRobotEvent target) {
        //This method manages the fire power
        double firePower;
        //Else, choose firePower based on distance to target
        if (target.getDistance() < 175) {
            firePower = 3;
        }
        else if(target.getDistance() < 300) {
            firePower = 2;
        }
        else {
            firePower = 1;
        }
        //If energy level drops too low, save power...
        if (getEnergy() < 20) {
            firePower = 0.1;
        }
        if (target.getEnergy() < 3.0) { //...except to get that greedy kill
            firePower = target.getEnergy() + 0.1;
        }
        return firePower;
    }

    private void charge(double absBearing) {
        //recklessly charge at low energy target
        setTurnRightRadians(absBearing - getHeadingRadians());
        setAhead(100);
    }

    private void getOutOfEdge() {
        //Moves away from edges, where radar range could possibly
        //not scan enemies too far away
        if (getX() < 150) {
            turnLeftRadians(Math.PI/2);
            setAhead(100);
        }
        else if (getX() > getBattleFieldWidth() - 150) {
            turnLeftRadians(3 * Math.PI/2);
            setAhead(100);
        }
        if (getY() < 150) {
            turnLeftRadians(0);
            setAhead(100);
        }
        else if (getY() > getBattleFieldHeight() - 150) {
            turnLeftRadians(Math.PI);
            setAhead(100);
        }
    }
}
