/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

/*
 * StrategicBombingRaidBattle.java
 *
 * Created on November 29, 2001, 2:21 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.net.GUID;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.PlayerAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.ui.display.ITripleaDisplay;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


/**
 * @author Sean Bridges
 * @version 1.0
 */
public class StrategicBombingRaidBattle implements Battle
{

    private final static String RAID = "Strategic bombing raid";
    private final static String FIRE_AA = "Fire AA";

    private Territory m_battleSite;
    private List<Unit> m_units = new ArrayList<Unit>();
    private Collection<Unit> m_targets = new ArrayList<Unit>();
    
    private PlayerID m_defender;
    private PlayerID m_attacker;
    private GameData m_data;
    private BattleTracker m_tracker;
    private boolean m_isOver = false;

    private final GUID m_battleID = new GUID();
    
    private final ExecutionStack m_stack = new ExecutionStack();
    private List<String> m_steps;
    private int m_bombingRaidCost;
    

    /** Creates new StrategicBombingRaidBattle */
    public StrategicBombingRaidBattle(Territory territory, GameData data, PlayerID attacker, PlayerID defender, BattleTracker tracker)
    {
        m_battleSite = territory;
        m_data = data;
        m_attacker = attacker;
        m_defender = defender;
        m_tracker = tracker;
    }

    /** Creates new StrategicBombingRaidBattle */
    public StrategicBombingRaidBattle(Territory territory, GameData data, PlayerID attacker, PlayerID defender, BattleTracker tracker, Collection<Unit> targets)
    {
        m_battleSite = territory;
        m_data = data;
        m_attacker = attacker;
        m_defender = defender;
        m_tracker = tracker;
        m_targets = targets;
    }
    /**
     * @param bridge
     * @return
     */
    private ITripleaDisplay getDisplay(IDelegateBridge bridge)
    {
        return (ITripleaDisplay) bridge.getDisplayChannelBroadcaster();
    }

    @Override
	public boolean isOver()
    {
        return m_isOver;
    }

    @Override
	public boolean isEmpty()
    {
        return m_units.isEmpty();
    }

    @Override
	public void removeAttack(Route route, Collection<Unit> units)
    {
        m_units.removeAll(units);
    }

    @Override
	public Change addAttackChange(Route route, Collection<Unit> units)
    {

        if (!Match.allMatch(units, Matches.UnitIsStrategicBomber))
            throw new IllegalArgumentException("Non bombers added to strategic bombing raid:" + units);

        m_units.addAll(units);
        return ChangeFactory.EMPTY_CHANGE;
    }

    @Override
	public Change addCombatChange(Route route, Collection<Unit> units, PlayerID player)
    {
        m_units.addAll(units);
        return ChangeFactory.EMPTY_CHANGE;
    }

    
    
    @Override
	public void fight(IDelegateBridge bridge)
    {
        //we were interrupted
        if(m_stack.isExecuting())
        {
            showBattle(bridge);
            m_stack.execute(bridge);
            return;
        }
            
       
        bridge.getHistoryWriter().startEvent("Strategic bombing raid in " + m_battleSite);
        bridge.getHistoryWriter().setRenderingData(m_battleSite);
        BattleCalculator.sortPreBattle(m_units, m_data);

        // TODO: determine if the target has the property, not just any unit with the property isAAforBombingThisUnitOnly
        boolean hasAA = m_battleSite.getUnits().someMatch(Matches.unitIsEnemyAAforBombing(m_attacker, m_data));
        
        m_steps = new ArrayList<String>();
        if (hasAA)
            m_steps.add(FIRE_AA);
        m_steps.add(RAID);

        showBattle(bridge);

        List<IExecutable> steps = new ArrayList<IExecutable>();
        
        
        if (hasAA)
            steps.add(new FireAA());

        steps.add(new ConductAA());
        
        steps.add(new IExecutable()
        {
        
            @Override
			public void execute(ExecutionStack stack, IDelegateBridge bridge)
            {
                getDisplay(bridge).gotoBattleStep(m_battleID, RAID);
                
                m_tracker.removeBattle(StrategicBombingRaidBattle.this);
//TODO Kev add unitDamage setting here
                if (isSBRAffectsUnitProduction())
                	bridge.getHistoryWriter().addChildToEvent("AA raid costs " + m_bombingRaidCost + " " + " production in " + m_battleSite.getName());
                else if(isDamageFromBombingDoneToUnitsInsteadOfTerritories())
                	bridge.getHistoryWriter().addChildToEvent("Bombing raid in " + m_battleSite.getName() + " causes " + m_bombingRaidCost + " " + " damage to " + m_targets.iterator().next());
                else
                	bridge.getHistoryWriter().addChildToEvent("AA raid costs " + m_bombingRaidCost + " " + MyFormatter.pluralize("PU", m_bombingRaidCost));
                //TODO  remove the reference to the constant.japanese- replace with a rule
                if(isPacificTheater() || isSBRVictoryPoints())
                {
                    if(m_defender.getName().equals(Constants.JAPANESE)) 
                    {
                        Change changeVP;
                        PlayerAttachment pa = (PlayerAttachment) PlayerAttachment.get(m_defender);
                        if(pa != null)
                        {
                            changeVP = ChangeFactory.attachmentPropertyChange(pa, (new Integer(-(m_bombingRaidCost / 10) + Integer.parseInt(pa.getVps()))).toString(), "vps");
                            bridge.addChange(changeVP);
                            bridge.getHistoryWriter().addChildToEvent("AA raid costs " + (m_bombingRaidCost / 10) + " " + MyFormatter.pluralize("vp", (m_bombingRaidCost / 10)));
                        } 
                    } 
                }
                
                // kill any suicide attackers (veqryn)
                if (Match.someMatch(m_units, Matches.UnitIsSuicide))
                {
                	List<Unit> suicideUnits = Match.getMatches(m_units, Matches.UnitIsSuicide);

                    m_units.removeAll(suicideUnits);
                    Change removeSuicide = ChangeFactory.removeUnits(m_battleSite, suicideUnits);
                    String transcriptText = MyFormatter.unitsToText(suicideUnits) + " lost in " + m_battleSite.getName();
                    bridge.getHistoryWriter().addChildToEvent(transcriptText, suicideUnits);
                    bridge.addChange(removeSuicide);
                }
                
                // kill any units that can die if they have reached max damage (veqryn)
                if (Match.someMatch(m_targets, Matches.UnitCanDieFromReachingMaxDamage))
                {
                	List<Unit> unitsCanDie = Match.getMatches(m_targets, Matches.UnitCanDieFromReachingMaxDamage);
                	unitsCanDie.retainAll(Match.getMatches(unitsCanDie, Matches.UnitIsAtMaxDamageOrNotCanBeDamaged(m_battleSite)));
                	if (!unitsCanDie.isEmpty())
                	{
                		//m_targets.removeAll(unitsCanDie);
                        Change removeDead = ChangeFactory.removeUnits(m_battleSite, unitsCanDie);
                        String transcriptText = MyFormatter.unitsToText(unitsCanDie) + " lost in " + m_battleSite.getName();
                        bridge.getHistoryWriter().addChildToEvent(transcriptText, unitsCanDie);
                        bridge.addChange(removeDead);
                	}
                }
            }
        });
       
        
        steps.add(new IExecutable()
        {
        
            @Override
			public void execute(ExecutionStack stack, IDelegateBridge bridge)
            { 
                if(isSBRAffectsUnitProduction())
                    getDisplay(bridge).battleEnd(m_battleID, "Bombing raid cost " + m_bombingRaidCost + " production.");
                else if(isDamageFromBombingDoneToUnitsInsteadOfTerritories())
                	getDisplay(bridge).battleEnd(m_battleID, "Bombing raid causes " + m_bombingRaidCost + " damage.");
                else
                    getDisplay(bridge).battleEnd(m_battleID, "Bombing raid cost " + m_bombingRaidCost + " " +  MyFormatter.pluralize("PU", m_bombingRaidCost));
                m_isOver = true;        
            }
        
        });


        Collections.reverse(steps);
        for (IExecutable executable : steps)
        {
            m_stack.push(executable);
        }
        m_stack.execute(bridge);
        
        
    }

    private void showBattle(IDelegateBridge bridge)
    {
        String title = "Bombing raid in " + m_battleSite.getName();
        getDisplay(bridge).showBattle(m_battleID, m_battleSite, title, m_units, getDefendingUnits(), Collections.<Unit, Collection<Unit>>emptyMap(), m_attacker, m_defender);
        getDisplay(bridge).listBattleSteps(m_battleID, m_steps);
    }

    @Override
	public List<Unit> getDefendingUnits()
    {
    	Match<Unit> defenders = new CompositeMatchOr<Unit>(Matches.UnitIsAA, Matches.UnitIsAtMaxDamageOrNotCanBeDamaged(m_battleSite).invert());
    	if(m_targets.isEmpty())
    		return Match.getMatches(m_battleSite.getUnits().getUnits(), defenders);
    	else
    	{
    		List<Unit> targets = Match.getMatches(m_battleSite.getUnits().getUnits(), Matches.UnitIsAAforBombing);
    		targets.addAll(m_targets);
    		return targets;
    	}	
    }

    @Override
	public List<Unit> getAttackingUnits()
    {
        return m_units;
    }

    class FireAA implements IExecutable
    {
        DiceRoll m_dice;
        Collection<Unit> m_casualties;
        
        @Override
		public void execute(ExecutionStack stack, IDelegateBridge bridge)
        {
            boolean isEditMode = EditDelegate.getEditMode(bridge.getData());

            IExecutable roll = new IExecutable()
            {
                @Override
				public void execute(ExecutionStack stack, IDelegateBridge bridge)
                {
                    m_dice = DiceRoll.rollAA(m_units, bridge, m_battleSite, Matches.UnitIsAAforBombing);
                }
            };

            IExecutable calculateCasualties = new IExecutable()
            {
            
                @Override
				public void execute(ExecutionStack stack, IDelegateBridge bridge)
                {
                    m_casualties = calculateCasualties(bridge, m_dice);
            
                }
            
            };
            
            IExecutable notifyCasualties = new IExecutable()
            {
            
                @Override
				public void execute(ExecutionStack stack, IDelegateBridge bridge)
                {
                    notifyAAHits(bridge, m_dice, m_casualties);
            
                }
            
            };

            
            IExecutable removeHits = new IExecutable()
            {

                @Override
				public void execute(ExecutionStack stack, IDelegateBridge bridge)
                {
                    removeAAHits(bridge, m_dice, m_casualties);
                }
                
            };
            
            //push in reverse order of execution
            stack.push(removeHits);
            stack.push(notifyCasualties);
            stack.push(calculateCasualties);
            if (!isEditMode)
                stack.push(roll);
            
        }

    }
    
    /**
     * @return
     */
    private boolean isSBRAffectsUnitProduction()
	{
    	return games.strategy.triplea.Properties.getSBRAffectsUnitProduction(m_data);
	}	
	
    private boolean isDamageFromBombingDoneToUnitsInsteadOfTerritories()
	{
    	return games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(m_data);
	}
    
    /**
     * @return
     */
    private boolean isWW2V2()
    {
    	return games.strategy.triplea.Properties.getWW2V2(m_data);
    }

    private boolean isLimitSBRDamageToProduction()
    {
    	return games.strategy.triplea.Properties.getLimitSBRDamageToProduction(m_data);
    }
    
	private boolean isLimitSBRDamagePerTurn(GameData data)
	{
    	return games.strategy.triplea.Properties.getLimitSBRDamagePerTurn(data);
	}
	   
    private ITripleaPlayer getRemote(IDelegateBridge bridge)
    {
        return (ITripleaPlayer) bridge.getRemote();
    }
    
	private boolean isPUCap(GameData data)
	{
    	return games.strategy.triplea.Properties.getPUCap(data);
	}
	
    private boolean isSBRVictoryPoints()
    {
        return games.strategy.triplea.Properties.getSBRVictoryPoint(m_data);
    }
    
    private boolean isPacificTheater()
    {
        return games.strategy.triplea.Properties.getPacificTheater(m_data);
    }

    private Collection<Unit> calculateCasualties(IDelegateBridge bridge, DiceRoll dice)
    {
        Collection<Unit> casualties = null;
        boolean isEditMode = EditDelegate.getEditMode(m_data);
        if (isEditMode)
        {
            String text = "AA guns fire";
            CasualtyDetails casualtySelection = BattleCalculator.selectCasualties(RAID, m_attacker, 
                    m_units, bridge, text, /* dice */null,/* defending */false, m_battleID, /* headless */false, 0);
            return casualtySelection.getKilled();
        }
        else
        {
            casualties = BattleCalculator.getAACasualties(m_units, dice, bridge, m_defender, m_attacker, m_battleID, m_battleSite, Matches.UnitIsAAforBombing);
        }
    	
        if (casualties.size() != dice.getHits())
            throw new IllegalStateException("Wrong number of casualties, expecting:" + dice + " but got:" + casualties);
        
        return casualties;
    }

    private void notifyAAHits(final IDelegateBridge bridge, DiceRoll dice, Collection<Unit> casualties)
    {
        getDisplay(bridge).casualtyNotification(m_battleID, FIRE_AA, dice, m_attacker, casualties, Collections.<Unit>emptyList(), Collections.<Unit, Collection<Unit>>emptyMap());
        
        Runnable r = new Runnable()
        {
            @Override
			public void run()
            {
                ITripleaPlayer defender = (ITripleaPlayer) bridge.getRemote(m_defender);
                defender.confirmEnemyCasualties(m_battleID, "Press space to continue", m_attacker);        
            }
        };
        Thread t = new Thread(r, "click to continue waiter");
        t.start();
        
        ITripleaPlayer attacker = (ITripleaPlayer) bridge.getRemote(m_attacker);
        attacker.confirmOwnCasualties(m_battleID, "Press space to continue");
        
        try
        {
            bridge.leaveDelegateExecution();
            t.join();
        } catch (InterruptedException e)
        {
          //ignore
        }
        finally
        {
            bridge.enterDelegateExecution();
        }
                
    }
    
    private void removeAAHits(IDelegateBridge bridge, DiceRoll dice, Collection<Unit> casualties)
    {
        if(!casualties.isEmpty())
            bridge.getHistoryWriter().addChildToEvent(MyFormatter.unitsToTextNoOwner(casualties) + " killed by AA guns", casualties);

        m_units.removeAll(casualties);
        Change remove = ChangeFactory.removeUnits(m_battleSite, casualties);
        bridge.addChange(remove);
    }

    class ConductAA implements IExecutable
    {
        private int[] m_dice;
        
        @Override
		public void execute(ExecutionStack stack, IDelegateBridge bridge)
        {
            IExecutable rollDice = new IExecutable()
            {
            
                @Override
				public void execute(ExecutionStack stack, IDelegateBridge bridge)
                {
                    rollDice(bridge);
                }
            
            };
            
            IExecutable findCost = new IExecutable()
            {
                @Override
				public void execute(ExecutionStack stack, IDelegateBridge bridge)
                {
                    findCost(bridge);
                }
                
            };
            
            //push in reverse order of execution
            m_stack.push(findCost);
            m_stack.push(rollDice);
            
            
        }
        
        private void rollDice(IDelegateBridge bridge)
        {
            int rollCount = BattleCalculator.getRolls(m_units, m_battleSite, m_attacker, false);
            if (rollCount == 0)
            {
                m_dice = null;
                return;
            }
            m_dice = new int[rollCount];
    
            boolean isEditMode = EditDelegate.getEditMode(m_data);
            if (isEditMode)
            {
                String annotation = m_attacker.getName() + " fixing dice to allocate cost of strategic bombing raid against " + m_defender.getName() + " in "
                        + m_battleSite.getName();
                ITripleaPlayer attacker = (ITripleaPlayer) bridge.getRemote(m_attacker);
                m_dice = attacker.selectFixedDice(rollCount, 0, true, annotation, m_data.getDiceSides()); // does not take into account bombers with dice sides higher than getDiceSides
            }
            else
            {
                String annotation = m_attacker.getName() + " rolling to allocate cost of strategic bombing raid against " + m_defender.getName() + " in "
                        + m_battleSite.getName();
                if (!games.strategy.triplea.Properties.getLL_DAMAGE_ONLY(m_data))
                	m_dice = bridge.getRandom(m_data.getDiceSides(), rollCount, annotation);
                else
                {
                	int i = 0;
            		int diceSides = m_data.getDiceSides();
                	for (Unit u : m_units)
                	{
                		UnitAttachment ua = UnitAttachment.get(u.getType());
                		int maxDice = ua.getBombingMaxDieSides();
                		int bonus = ua.getBombingBonus();
                		if (maxDice < 0 && bonus < 0 && diceSides >= 5)
                		{
                			maxDice = (diceSides+1)/3;
                			bonus = (diceSides+1)/3;
                		}
                		if (bonus < 0)
                			bonus = 0;
                		if (maxDice < 0)
                			maxDice = diceSides;
                		if (maxDice > 0)
                    		m_dice[i] = bridge.getRandom(maxDice, annotation) + bonus;
                		else
                			m_dice[i] = bonus;
                		i++;
                	}
                }
            }
        }
        
        private void findCost(IDelegateBridge bridge)
        {
            //if no planes left after aa fires, this is possible
            if(m_units.isEmpty())
            {
                return;
            }
            
            TerritoryAttachment ta = TerritoryAttachment.get(m_battleSite);
            int cost = 0;
            boolean lhtrHeavyBombers = games.strategy.triplea.Properties.getLHTR_Heavy_Bombers(m_data);
            //boolean lhtrHeavyBombers = m_data.getProperties().get(Constants.LHTR_HEAVY_BOMBERS, false);
            
            int damageLimit = ta.getProduction();
            
            Iterator<Unit> iter = m_units.iterator();
            int index = 0;
            Boolean limitDamage = isWW2V2() || isLimitSBRDamageToProduction();
            
            // limit to maxDamage
            while (iter.hasNext())
            {
                int rolls;
                
                rolls = BattleCalculator.getRolls(iter.next(), m_battleSite, m_attacker, false);
                
                int costThisUnit = 0;
                
                if(lhtrHeavyBombers && rolls > 1)
                {
                    int max = 0;
                    for(int i =0; i < rolls; i++)
                    {
                        //+2 since 0 based (LHTR adds +1 to base roll)
                        max = Math.max(max, m_dice[index]  + 2);
                        index++;
                    }

                    costThisUnit = max;
                    
                }
                else
                {
                    for (int i = 0; i < rolls; i++)
                    {
                        costThisUnit += m_dice[index] + 1;
                        index++;
                    }                    
                }
                

                if (limitDamage)
                    cost += Math.min(costThisUnit, damageLimit);
                else
                    cost += costThisUnit;
            }
            
            // Limit PUs lost if we would like to cap PUs lost at territory value
        	if (isPUCap(m_data) || isLimitSBRDamagePerTurn(m_data))
        	{
        		int alreadyLost = DelegateFinder.moveDelegate(m_data).PUsAlreadyLost(m_battleSite);
        		int limit = Math.max(0, damageLimit - alreadyLost);
        		cost = Math.min(cost, limit);
        	}

            //If we damage units instead of territories
            if(isDamageFromBombingDoneToUnitsInsteadOfTerritories() && !isSBRAffectsUnitProduction())
            {
            	//determine the max allowed damage
            	Unit current = m_targets.iterator().next();
                UnitAttachment ua = UnitAttachment.get(current.getType());
                TripleAUnit taUnit = (TripleAUnit) current;
                
            	damageLimit = taUnit.getHowMuchMoreDamageCanThisUnitTake(current, m_battleSite);
            	cost = Math.min(cost, damageLimit);
            	int totalDamage = taUnit.getUnitDamage() + cost;

            	//display the results
        		getDisplay(bridge).bombingResults(m_battleID, m_dice, cost);

            	// Record production lost
            	DelegateFinder.moveDelegate(m_data).PUsLost(m_battleSite, cost);
        		
        		//apply the hits to the targets
        		IntegerMap<Unit> hits = new IntegerMap<Unit>();
        		CompositeChange change = new CompositeChange();
        		
        		for(Unit unit:m_targets)
            	{
            		hits.put(unit,1);
                    change.add(ChangeFactory.unitPropertyChange(unit, totalDamage, TripleAUnit.UNIT_DAMAGE));
                    //taUnit.setUnitDamage(totalDamage);
            	}
            	bridge.addChange(ChangeFactory.unitsHit(hits));
            	bridge.addChange(change);
            	
                bridge.getHistoryWriter().startEvent("Bombing raid in " + m_battleSite.getName() + " causes: " + cost + " damage.");
            	getRemote(bridge).reportMessage("Bombing raid in " + m_battleSite.getName() + " causes: " + cost + " damage.");
            }
            else if(isSBRAffectsUnitProduction())
            {
            	// the old way of doing it, based on doing damage to the territory rather than the unit
            	//get current production
                int unitProduction = ta.getUnitProduction();
            	//Determine the max that can be taken as losses
                int alreadyLost = damageLimit - unitProduction;
                
                int limit = 2 * damageLimit  - alreadyLost;
                cost = Math.min(cost, limit);
        		
        		getDisplay(bridge).bombingResults(m_battleID, m_dice, cost);

            	// Record production lost
            	DelegateFinder.moveDelegate(m_data).PUsLost(m_battleSite, cost);
            	
            	Collection<Unit> damagedFactory = Match.getMatches(m_battleSite.getUnits().getUnits(), Matches.UnitIsFactoryOrCanBeDamaged);

        		IntegerMap<Unit> hits = new IntegerMap<Unit>();
            	for(Unit factory:damagedFactory)
            	{
            		hits.put(factory,1);
            	}
            	//add a hit to the factory
            	bridge.addChange(ChangeFactory.unitsHit(hits));
            	
            	Integer newProduction = unitProduction - cost;
            	
            	//decrease the unitProduction capacity of the territory
                Change change = ChangeFactory.attachmentPropertyChange(ta, newProduction.toString(), "unitProduction");
            	bridge.addChange(change);
                bridge.getHistoryWriter().startEvent("Bombing raid in " + m_battleSite.getName() + " costs: " + cost + " production.");

            	getRemote(bridge).reportMessage("Bombing raid in " + m_battleSite.getName() + " costs: " + cost + " production.");
            }
            else
            {
            	// Record PUs lost
            	DelegateFinder.moveDelegate(m_data).PUsLost(m_battleSite, cost);
            	
            	cost *= Properties.getPU_Multiplier(m_data);
            	getDisplay(bridge).bombingResults(m_battleID, m_dice, cost);

            	//get resources
            	Resource PUs = m_data.getResourceList().getResource(Constants.PUS);
            	int have = m_defender.getResources().getQuantity(PUs);
            	int toRemove = Math.min(cost, have);
            	
            	Change change = ChangeFactory.changeResourcesChange(m_defender, PUs, -toRemove);
            	bridge.addChange(change);            	
            }

            m_bombingRaidCost = cost;
               
        }   
    }

    @Override
	public boolean isBombingRun()
    {

        return true;
    }

    @Override
	public void unitsLostInPrecedingBattle(Battle battle, Collection<Unit> units, IDelegateBridge bridge)
    {
        //should never happen
        throw new IllegalStateException("say what, why you telling me that");
    }

    @Override
	public int hashCode()
    {

        return m_battleSite.hashCode();
    }

    @Override
	public boolean equals(Object o)
    {

        //2 battles are equal if they are both the same type (boming or not)
        //and occur on the same territory
        //equals in the sense that they should never occupy the same Set
        //if these conditions are met
        if (o == null || !(o instanceof Battle))
            return false;

        Battle other = (Battle) o;
        return other.getTerritory().equals(this.m_battleSite) && other.isBombingRun() == this.isBombingRun();
    }

    @Override
	public Territory getTerritory()
    {

        return m_battleSite;
    }

    
    @Override
	public Collection<Unit> getDependentUnits(Collection<Unit> units)
    {
        return Collections.emptyList();
    }

    /**
     * Add bombarding unit. Doesn't make sense here so just do nothing.
     */
    @Override
	public void addBombardingUnit(Unit unit)
    {
        // nothing
    }

    /**
     * Return whether battle is amphibious.
     */
    @Override
	public boolean isAmphibious()
    {
        return false;
    }

    @Override
	public Collection<Unit> getAmphibiousLandAttackers()
    {
        return new ArrayList<Unit>();
    }

    @Override
	public Collection<Unit> getBombardingUnits()
    {
        return new ArrayList<Unit>();
    }
    
    @Override
	public int getBattleRound()
    {
        return 0;
    }
    
}
