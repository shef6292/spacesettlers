
package shef6292;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.DoNothingAction;
import spacesettlers.actions.PurchaseCosts;
import spacesettlers.actions.PurchaseTypes;
import spacesettlers.clients.TeamClient;
import spacesettlers.graphics.CircleGraphics;
import spacesettlers.graphics.LineGraphics;
import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.objects.AbstractActionableObject;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Base;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.objects.powerups.SpaceSettlersPowerupEnum;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.objects.weapons.AbstractWeapon;
import spacesettlers.objects.weapons.Missile;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;

/**
 *
 * @author Chris Brown and Dillon Sheffield
 */

/*
    This class contains the code for HunterBot. HunterBot is an agent which focuses on hunting the nearest
    enemy ships, but also will grab energy beacons if they are convenient or if HunterBot is low on energy.
    HunterBot attacks enemy ships by predicting a firing solution, and thus firing at the place where they 
    will be.
*/
public class HunterBot extends TeamClient {
        private HashSet<SpacewarGraphics> graphics;
        private AbstractObject target; //This is what we are aiming for
        private boolean clearedToFire; //This is true if we have a target, and it is a target we want to shoot at; it is false if we don't want to shoot
        private static final int MINIMUM_ENERGY = 0; //A hard minimum on energy reserves, below this level we run for a beacon
        private static final int MINIMUM_BASE_ENERGY = 500; //Bases with energy reserves below this value will not be considered  valid locations for refueling
        private int lastShotTime = -100; //The time the last shot was fired, used for calculating whether it has been long enough to fire another shot
        private static final double STEPS_PER_SECOND = 20; //The number of physics steps per second; this is a double to allow for, say, 1 step every 2 seconds, to be represented if necessary
        private static final double CLOSE_RANGE_THRESHOLD = 100; //The distance at which we begin firing at close to the maximum rate
        private static final double MAX_ENERGY_TO_SEARCH_FACTOR = 7.0/10.0; //We will never search for energy when the ship has more than this proportion of its maximum energy
        private static final double MISSING_ENERGY_THRESHOLD = 7.0/10.0; //When the ship has more than this proportion of its life MISSING, it will prefer finding energy beacons over hunting ships, given an equivalent distance, and vice versa when the ship has less than that proportion missing
        private static final double STAND_OFF_DISTANCE = 40; // The distance that we will try and keep between us and our target ship
        private static final double STAND_OFF_ROTATION = Math.PI/12; //The angle that we try and rotate our angle about the target ship by
        private static final double KP_ROTATIONAL = 20;     //The constant value that we will set the rotational Kp value to
        private static final double KV_ROTATIONAL = 12;     //The constant value that we will set the rotational Kv value to
        private static final double KP_TRANSLATIONAL = 0.2f;  //The constant value that we will set the translational Kp value to
        private static final double KV_TRANSLATIONAL = 0.894f;  //The constant value that we will set the translational Kv value to
        private static final int SCALE = 40;    // The scale of the search grid
        private Stack<Position> plannedPath; // Our planned path
        private Position currentMoveTarget; // The position that we are presently moving towards
        private static final int SEARCH_DELAY = 10; // The delay between searches
        private static final double VELOCITY_CONSTANT = 0.15; //The coefficient of the distance to goal term, for determining the desired velocity at the end of a path leg
        private static final double DISTANCE_POWER = 0.8; //The power to which we raise the distance to the goal, for determining the desired velocity at the end of a path leg
        private static final boolean GRAPHICS_ENABLED = true; //Whether we want to be able to view graphical output
        
	@Override
	public void initialize(Toroidal2DPhysics space) {
            graphics = new HashSet<SpacewarGraphics>();
            target = null; //We have no target to begin with
            clearedToFire = false; //Because of this, we don't want to shoot
	}

	@Override
	public void shutDown(Toroidal2DPhysics space) {
		// TODO Auto-generated method stub

	}
        
        private AbstractObject getNearestFoeObject(Toroidal2DPhysics space, Ship myShip) {
            //Choose a target to shoot at
            double bestShipDistance = 50000; //Start with an absurdly high distance, so that any other distance should be below it
            Ship bestShip = null; //We don't know what the closest ship is yet
            for(Ship ship:space.getShips()) {
                if(!ship.getTeamName().equals(myShip.getTeamName()) && ship.isAlive()) {
                    //It is an enemy ship
                    if(bestShip == null) {
                        //This is the first and  thus closest enemy that we've seen
                        bestShip = ship; //We update the variables accordingly
                        bestShipDistance = space.findShortestDistance(ship.getPosition(), myShip.getPosition());
                    } else {
                        if(space.findShortestDistance(ship.getPosition(), myShip.getPosition()) < bestShipDistance) {
                            //Closer than the previous closest enemy, so we update the variables
                            bestShip = ship;
                            bestShipDistance = space.findShortestDistance(ship.getPosition(), myShip.getPosition());
                        }
                    }
                } else {
                    //It is a friendly ship
                    //Continue
                }
            }
            //We return the closest ship that we've found
            //This may be null, if all enemy ships are respawning
            return bestShip;
        }
        
        private AbstractObject getNearestEnergyObject(Toroidal2DPhysics space, Ship myShip) {
            //We want the best energy object to refuel at
            //Find the closest beacon
            double bestBeaconDistance = 50000; //We pick something absurdly large, so that any real distance will be smaller
            Beacon bestBeacon = null; //We haven't found a beacon yet
            for(Beacon beacon:space.getBeacons()) {
                if(bestBeacon == null) {
                    //First and  thus closest beacon that we've seen
                    bestBeacon = beacon; //We update the variables accordingly
                    bestBeaconDistance = space.findShortestDistance(beacon.getPosition(), myShip.getPosition());
                } else {
                    if(space.findShortestDistance(beacon.getPosition(), myShip.getPosition()) < bestBeaconDistance) {
                        //Closer than the previous closest beacon
                        bestBeacon = beacon; //So it is now the new closest beacon
                        bestBeaconDistance = space.findShortestDistance(beacon.getPosition(), myShip.getPosition());
                    }
                }
            }
            //Find the closest base
            double bestBaseDistance = 50000; //We pick something absurdly large, so that any real distance will be smaller
            Base bestBase = null; //We haven't found a base yet
            for(Base base:space.getBases()) {
                if(base.getTeamName().equals(myShip.getTeamName()) && base.getHealingEnergy() >= MINIMUM_BASE_ENERGY) {
                    //It is friendly, so we can refuel there
                    //And it has enough energy, so we are allowed to refuel there
                    if(bestBase == null) {
                        //First and  thus closest beacon that we've seen
                        bestBase = base; //We update the variables accordingly
                        bestBaseDistance = space.findShortestDistance(base.getPosition(), myShip.getPosition());
                    } else{
                        //Check the distance, weighted by available energy
                        if((space.findShortestDistance(base.getPosition(), myShip.getPosition())/((double) base.getHealingEnergy())) <
                                (bestBaseDistance/((double)bestBase.getHealingEnergy()))) {
                            //Closer than the previous closest beacon
                            //Or has better energy content
                            bestBase = base; //So it is now the new closest beacon
                            bestBaseDistance = space.findShortestDistance(base.getPosition(), myShip.getPosition());
                        }
                    }
                } else {
                    //Either it is an enemy base
                    //And we can't refuel there
                    //Or it is too low on fuel
                    //And we won't refuel there
                    //Continue
                }
            }
            //If the closest base is closer, return that
            //Otherwise, return the closest beacon
            //Weight this by the healing amount
            //Note: we already have a minimum permissible base energy, so we aren't at risk of dividing by zero
            if(bestBase != null && (((double)bestBaseDistance)/((double) bestBase.getHealingEnergy())) < 
                    (((double)bestBeaconDistance)/((double)Beacon.BEACON_ENERGY_BOOST))) {
                return bestBase;
            } else {
                return bestBeacon;
            }
        }
        // Return the position the launcher should shoot at to hit the target
        private Position getFiringSolution(AbstractObject target, Ship launcher, Toroidal2DPhysics space) {
            if(!target.isMoveable()) {
                //The target is a sitting duck and we can just head right for it without calculation
                clearedToFire = true;
                return target.getPosition();
            } else {
                Position launcherPos = launcher.getPosition(); //The launcher's position
                Position targetPos = target.getPosition(); //The target's postion
                Vector2D interceptVector; //The relative position we want to shoot at
                Position interceptPos; //The absolute position we want to shoot at
                //The initial position of the target relative to the launcher
                Vector2D relPosNot = space.findShortestDistanceVector(launcher.getPosition(), target.getPosition()); 
                Vector2D targetVel = targetPos.getTranslationalVelocity(); //The target's current velocity
                //We are going to calculate the time to intercept first
                //Then we will use that to calculate the position
                //The portion of the equation under the radical
                double interceptRadical = 4*Math.pow(relPosNot.dot(targetVel), 2) + 
                        4*(Math.pow(Missile.INITIAL_VELOCITY, 2) - targetVel.dot(targetVel))*(relPosNot.dot(relPosNot));
                if(interceptRadical < 0) {
                    //There is no solution
                    clearedToFire = false; //Hold fire
                    return targetPos; //Move towards the target's present position instead
                } else {
                    double interceptDenom = 2*(Math.pow(Missile.INITIAL_VELOCITY, 2) - targetVel.dot(targetVel));
                    if(Math.abs(interceptDenom) < 0.00001) {
                        //The denominator is too close to 0, there is no solution
                        //Hold fire and move towards the target's current position
                        clearedToFire = false;
                        return targetPos;
                    } else {
                        double interceptNonradical = 2*relPosNot.dot(targetVel); // The part of the numerator not under the radical
                        interceptRadical = Math.sqrt(interceptRadical); //We finish taking the root of the radical, since it is legal
                        double interceptTime; //The time our missile will hit
                        if(interceptNonradical < interceptRadical) {
                            //We must add the radical to the nonradical, as we cannot have a negative time
                            interceptTime = (interceptNonradical + interceptRadical)/interceptDenom;
                        } else {
                            //We want the lower time, subtract the radical
                            interceptTime = (interceptNonradical - interceptRadical)/interceptDenom;
                        }
                        interceptVector = relPosNot.add(targetVel.multiply(interceptTime));
                        interceptPos = new Position(interceptVector.add(new Vector2D(launcherPos.getX(), launcherPos.getY())));
                        clearedToFire = true;
                        return interceptPos;
                    }
                }
            }
        }
        
        //Returns the delay between shots due to the range
        private int getRangeDelay(double dist, Ship ship) {
            if(dist < ship.getRadius()*2 + Missile.MISSILE_RADIUS*2) {
                //We are practically on top of them
                //(Our missiles will spawn inside of them)
                //Fire at the maximum rate
                return 1;
            }
            else if (dist < CLOSE_RANGE_THRESHOLD) {
                //We are at very close range
                //Maximizing damage is much more important than minimizing losses to dodging
                return 2;
            } else {
                //We want all of the shots we can shoot, evenly spaced between us and the target
                //This is so that we won't lose an entire salvo to one dodging action
                double seperation = (dist/(double)ship.getWeaponCapacity());
                double floatTime = STEPS_PER_SECOND*(seperation/(double)Missile.INITIAL_VELOCITY); //Converting this seperation to a time, in steps
                return (int) floatTime + 1; //We round up, to be sure to avoid intercepting our own missiles
            }
        }
        
        private int getSpeedDelay(double dist, Ship ship, Toroidal2DPhysics space) {
            if(dist < ship.getRadius()*3) {
                //We are practically on top of them
                //Fire at the maximum rate
                return 1;
            } else /*if(Math.abs(ship.getPosition().getOrientation() - ship.getPosition().getTranslationalVelocity().getAngle()) <= 
                    Math.atan(((double)2*Missile.MISSILE_RADIUS)/((double)(ship.getRadius() + 2*Missile.MISSILE_RADIUS))))*/{
                //We want to avoid stacking shots on top of each other
                //This is so that we won't lose missiles to our own missiles
                //We slow our rate of fire, because we are pointed closely enought to the direction of travel that this is an issue
                double seperation = 2*Missile.MISSILE_RADIUS;
                double floatTime = STEPS_PER_SECOND*(seperation/((double)Missile.INITIAL_VELOCITY - 
                        ship.getPosition().getTranslationalVelocity().dot(space.findShortestDistanceVector(ship.getPosition(), getFiringSolution(target, ship, space)).unit()))); //Converting this seperation to a time, in steps
                return (int) floatTime + 1; //We round up, to be sure to avoid intercepting our own missiles
            } /*else {
                //We aren't pointed in the direction of our velocity
                //Fire as much as we can
                return 1;
            }*/
        }
        
        private void updatePath(Position start, Position goal, Toroidal2DPhysics space) {           
            //Create a new graph for navigation
            GridGraph graph = new GridGraph(start, goal, space, SCALE);
            
            //Search the graph and get a path
            A_Star_Search search = new A_Star_Search(graph, graph.getStart(), graph.getGoal());
            AStarTreeNode[] treePath = search.search();
            
            if(treePath != null) {
                //Empty the stack
                plannedPath = new Stack<Position>();
                
                //Iterate through the path, from the goal
                for(int i = treePath.length - 1; i >= 0; i--) {
                    //Extract the position from the node
                    Position pos = treePath[i].getMyNode().getPosition();
                    
                    if(i < treePath.length - 1) {
                        //Draw a line graphic
                        SpacewarGraphics graphic = new LineGraphics(treePath[i+1].getMyNode().getPosition(), treePath[i].getMyNode().getPosition(), 
                                space.findShortestDistanceVector(treePath[i+1].getMyNode().getPosition(), treePath[i].getMyNode().getPosition()));
                        graphics.add(graphic);
                        
                        //Initialize the velocity
                        //The speed should be proportional to some power of the distance from the goal
                        Vector2D vec = space.findShortestDistanceVector(pos, 
                                treePath[i+1].getMyNode().getPosition()).getUnitVector().multiply(VELOCITY_CONSTANT*
                                        Math.pow(Math.abs(space.findShortestDistance(pos, goal)),
                                        DISTANCE_POWER));
                        pos.setTranslationalVelocity(vec);
                    } else {
                        pos.setTranslationalVelocity(Vector2D.ZERO_VECTOR);
                    }
                    
                    plannedPath.push(pos);
                }
            } else {
                //No path was found
                //Continue as previously planned
            }
        }
        
	@Override
	public Map<UUID, AbstractAction> getMovementStart(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
            
            //Reset the graphics
            graphics = new HashSet<SpacewarGraphics>();
            
            //Decide the movement of our ships (and bases, but those don't move)
            HashMap<UUID, AbstractAction> actions = new HashMap<UUID, AbstractAction>();
            for (AbstractObject actionable : actionableObjects) {
                if(actionable instanceof Ship)  {
                    Ship ship = (Ship) actionable;
                    AbstractAction current = ship.getCurrentAction();
                    Position orientTarget = null;
                    
                    if(!ship.isAlive()) {
                        //If out ship is dead, clear the path
                        plannedPath = new Stack<>();
                    }
                    else if(current == null || current.isMovementFinished(space) || (current instanceof MoveAndOrientAction &&
                            ((MoveAndOrientAction) current).isMovementFinished(ship, space)) || target == null || !target.isAlive() || plannedPath.isEmpty()) {
                        //If we are done with our current movement or if we didn't have a current movement
                        //Or if we are chasing a dead ship
                        //Or if we aren't chasing anything
                        //Or if our path is empty
                        //Then pick a new destination
                        
                        if(ship.getEnergy() < MINIMUM_ENERGY) {
                            //If we are low on energy, find a beacon and head to that
                            target = getNearestEnergyObject(space, ship);
                            //Disable shooting, because we don't want to shoot the beacon
                            clearedToFire = false;
                        } else {
                            //Find enemy objects, and destroy them
                            target = getNearestFoeObject(space, ship);
                            //But if there is nearby energy, take the opportunity to grab that instead
                            //But only if we are not topped off (we use 7/10 of max energy, or 3500 energy, as our threshold here)
                            //With certain powerups, it can be higher
                            if(ship.getEnergy() < (int)(MAX_ENERGY_TO_SEARCH_FACTOR*((double)ship.getMaxEnergy()))) {
                                AbstractObject energy = getNearestEnergyObject(space, ship);
                                //Go to the energy, if the ratio of its distance to the enemy distance is
                                //less than the ratio of maximum energy to missing energy times some constant value
                                if(space.findShortestDistance(ship.getPosition(), energy.getPosition())*MISSING_ENERGY_THRESHOLD*(
                                        ship.getMaxEnergy()/(ship.getMaxEnergy() - ship.getEnergy())) <
                                        space.findShortestDistance(ship.getPosition(), target.getPosition())) {
                                    target = energy; //Go get the beacon
                                    clearedToFire = false; //We don't want to shoot the beacon
                                }
                            }
                            //If the target is still something that we want to calculate a firing solution for, do so
                            if(!(target instanceof Beacon || target instanceof Base)){
                                orientTarget = getFiringSolution(target, ship, space);
                            }
                        }
                        
                        
                        Position translateTarget; //The position that we want to move to
                        if(target != null && !target.isMoveable()) {
                            //If we have a target, and it won't move, move to it
                            translateTarget = target.getPosition();
                            orientTarget = target.getPosition();
                        }
                        else if(orientTarget != null) { 
                            //Otherwise, move towards them
                            //We want to keep a stand off distance
                            //So move to someplace not directly on top of them
                            Vector2D relativePosition = space.findShortestDistanceVector(ship.getPosition(), target.getPosition());
                            Vector2D unitRelativePosition = relativePosition.getUnitVector();
                            
                            //Take the target's position, and move it some distance towards our ship
                            //And rotate it, so we won't sit still
                            Vector2D targetVector = new Vector2D(target.getPosition().getX(), target.getPosition().getY());
                            Vector2D shiftVector = unitRelativePosition.multiply(STAND_OFF_DISTANCE).rotate(STAND_OFF_ROTATION);
                            translateTarget = new Position(targetVector.subtract(shiftVector));
                        } else { 
                            //If we haven't got someplace to go, do nothing
                            translateTarget = ship.getPosition();
                            orientTarget = translateTarget;
                        }
                        
                        //Update the path if
                        //It is empty
                        //Or if enough time has elapsed
                        //Or if we have drifted far from our interim destination
                        //Also, let the movement execution know that the path has been updated
                        boolean updated = false;
                        if(plannedPath == null || (plannedPath.isEmpty() && (!(space.findShortestDistance(ship.getPosition(), translateTarget) <= 2*SCALE) ||
                                !(current instanceof MoveAndOrientAction))) || space.getCurrentTimestep()% SEARCH_DELAY == 0 || 
                                space.findShortestDistance(ship.getPosition(), currentMoveTarget) >= SCALE*3) {
                            updatePath(ship.getPosition(), translateTarget, space);
                            updated = true;
                        }
                        
                        if(orientTarget != null) {
                            SpacewarGraphics graphic = new CircleGraphics(1, getTeamColor(), orientTarget); //Add a circle at our destination
                            graphics.add(graphic);
                        }
                        
                        //Execute the movement
                        MoveAndOrientAction charge = null; //Move towards our destination
                        if(!plannedPath.isEmpty() && (current == null || (current instanceof MoveAndOrientAction && 
                                ((MoveAndOrientAction)current).isMovementFinished(ship, space)) || updated)) {
                            currentMoveTarget = plannedPath.pop();
                            SpacewarGraphics graphic = new CircleGraphics(1, getTeamColor(), currentMoveTarget); //Add a circle at our subdestination
                            graphics.add(graphic);
                            charge = new MoveAndOrientAction(space, ship.getPosition(), currentMoveTarget, orientTarget, currentMoveTarget.getTranslationalVelocity());
                        } else if (!(current instanceof MoveAndOrientAction)){
                            SpacewarGraphics graphic = new CircleGraphics(1, getTeamColor(), currentMoveTarget); //Add a circle at our subdestination
                            graphics.add(graphic);
                            charge = new MoveAndOrientAction(space, ship.getPosition(), currentMoveTarget, orientTarget, currentMoveTarget.getTranslationalVelocity());
                        } else {
                            //Do nothing
                        }
                        if(charge != null) {
                            charge.setKpRotational(KP_ROTATIONAL); //We want to turn more quickly, for more accurate shooting
                            charge.setKvRotational(KV_ROTATIONAL);  //To maintain critical damping, but tweaked upwards to allow for distortion from the discrete time steps
                            charge.setKpTranslational(KP_TRANSLATIONAL); //We want to move more quickly, as well
                            charge.setKvTranslational(KV_TRANSLATIONAL); //And we want to maintain translational critical damping

                            actions.put(ship.getId(), charge); //put the movement in our movement set
                        }
                    }
                } else {
                    actions.put(actionable.getId(), new DoNothingAction()); //If it isn't a ship, it cannot do anything
                }
            }
            return actions; //Send all of the actions back to the simulator
	}

	@Override
	public void getMovementEnd(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<SpacewarGraphics> getGraphics() {
            if(graphics.isEmpty() && GRAPHICS_ENABLED) {
                return null;
            } else {
                return graphics;
            }
	}


	@Override
	public Map<UUID, PurchaseTypes> getTeamPurchases(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects, 
			ResourcePile resourcesAvailable, 
			PurchaseCosts purchaseCosts) {
		return new HashMap<UUID,PurchaseTypes>(); //We make no purchases

	}

	@Override
	public Map<UUID, SpaceSettlersPowerupEnum> getPowerups(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
            //This controls shooting
            //We use no other powerups yet
            HashMap<UUID, SpaceSettlersPowerupEnum> powerupMap = new HashMap<UUID, SpaceSettlersPowerupEnum>();
            for(AbstractActionableObject actionableObject : actionableObjects) {
                if(actionableObject instanceof Ship) {
                    Ship ship = (Ship) actionableObject;
                    //The distance to the intercept that we shoot at
                    double distance;
                    if(target != null && target.isMoveable()) { //We check to make sure we have a target first
                        //If it moves, we want the distance to the intercept
                        //Note: this usage of the method may set clearedToFire to true
                        //This is not always appropriate, and should be fixed in a later version
                        //It automatically sets clearedToFire to true if the object cannot move
                        //Therefore, we use the if statement as a workaround
                        distance = space.findShortestDistance(ship.getPosition(), getFiringSolution(target, ship, space));
                    } else if(target != null){
                        //Otherwise, we want the distance to where it is now
                        distance = space.findShortestDistance(ship.getPosition(), target.getPosition());
                    } else {
                        //If we have no target, the distance doesn't matter
                        //But we'll initialize it anyway
                        distance = 50000;
                    }
                    //If we have permission to shoot
                    //and if we have a target
                    //and if the target is alive
                    //and if the timing is right to shoot
                    //and if we are actually pointed someplace where the shot should hit it
                    //and if there is actually a path clear of asteroids from the ship to the spot 
                    //and if we aren't going to shoot ourselves (i.e., by running over our missiles)
                    //Then, fire
                    //Note: the cast (Set) space.getAsteroids() is safe because every Asteroid is an AbstractObject
                    //Note: the last condition will apply even if the target ship would have collected the asteroids in question before 
                    //      the time of intercept
                    if(clearedToFire && target != null && target.isAlive() && (space.getCurrentTimestep() - lastShotTime) >= getRangeDelay(distance, ship) &&
                            (space.getCurrentTimestep() - lastShotTime) >= getSpeedDelay(distance, ship, space) && ship.getPosition().getTotalTranslationalVelocity() < Missile.INITIAL_VELOCITY &&
                            Math.abs(ship.getPosition().getOrientation() - 
                                    space.findShortestDistanceVector(ship.getPosition(), getFiringSolution(target, ship, space)).getAngle()) <=
                            Math.atan(Ship.SHIP_RADIUS/distance) &&
                            space.isPathClearOfObstructions(ship.getPosition(), getFiringSolution(target, ship, space),
                                    (Set) space.getAsteroids(), Missile.MISSILE_RADIUS)) {
                        AbstractWeapon newBullet = ship.getNewWeapon(SpaceSettlersPowerupEnum.FIRE_MISSILE);
                        if (newBullet != null) { //Unless we are out of bullets, then we do not fire
                                powerupMap.put(ship.getId(), SpaceSettlersPowerupEnum.FIRE_MISSILE);
                                lastShotTime = space.getCurrentTimestep(); //Allow for another delay before the next shot
                                //System.out.println("Firing!");
                        }
                    }
                }
            }
            return powerupMap; //Return the list of powerup activations
	}

}


