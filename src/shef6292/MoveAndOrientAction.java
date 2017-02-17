/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shef6292;

import spacesettlers.actions.AbstractAction;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Movement;
import spacesettlers.utilities.Position;
import spacesettlers.actions.MoveAction;
import spacesettlers.utilities.Vector2D;

/**
 *
 * @author chris
 */
public class MoveAndOrientAction extends AbstractAction{
    //Orient the ship to a given position, using MoveActions
    //This action uses two move actions
    //One to orient to a position
    //And one to move to a (presumable different) position
    MoveAction orientAction;
    MoveAction translateAction;
    Position goal;
    private static final double ACCEPTABLE_RADIUS = 2*Ship.SHIP_RADIUS; //The ship must be within this radius of the goal for the movement to be complete
    
    
    public MoveAndOrientAction(Toroidal2DPhysics space, Position currentLocation, Position targetLocation, Position orientLocation, Vector2D targetVelocity) {
        orientAction = new MoveAction(space, currentLocation, orientLocation);
        translateAction = new MoveAction(space, currentLocation, targetLocation, targetVelocity);
        goal = targetLocation;
    }
    
    public MoveAndOrientAction(Toroidal2DPhysics space, Position currentLocation, Position targetLocation, Position orientLocation) {
        orientAction = new MoveAction(space, currentLocation, orientLocation);
        translateAction = new MoveAction(space, currentLocation, targetLocation);
        goal = targetLocation;
    }
    
    @Override
    public boolean isMovementFinished(Toroidal2DPhysics space) {
        boolean finished = translateAction.isMovementFinished(space);
        return finished;
    }
    
    public boolean isMovementFinished(Ship ship, Toroidal2DPhysics space) {
        boolean finished = space.findShortestDistance(ship.getPosition(), goal) <= ACCEPTABLE_RADIUS;
        return finished;
    }
    
    @Override
    public Movement getMovement(Toroidal2DPhysics space, Ship ship) {
        //Set the rotational acceleration to match that from orientAction
        Movement move = orientAction.getMovement(space, ship);
        //Set the translational acceleration to match that from translateAction
        move.setTranslationalAcceleration(translateAction.getMovement(space, ship).getTranslationalAcceleration());
        return move;
    }
    
    public void setKpRotational(double kpRotational) {
        orientAction.setKpRotational(kpRotational);
    }
    public void setKvRotational(double kvRotational) {
        orientAction.setKvRotational(kvRotational);
    }
    public void setKpTranslational(double kpTranslational) {
        translateAction.setKpTranslational(kpTranslational);
    }
    public void setKvTranslational(double kvTranslational) {
        translateAction.setKvTranslational(kvTranslational);
    }
}
