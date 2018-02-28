package tracks.tutorialGeneration.AgentBasedGraphRepresentationGenerator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.json.simple.parser.ParseException;

import core.game.GameDescription;
import core.game.SLDescription;
import core.generator.AbstractTutorialGenerator;
import core.game.GameDescription.SpriteData;
import tools.ElapsedCpuTimer;
import tools.GameAnalyzer;
import tools.LevelAnalyzer;
import tracks.tutorialGeneration.VisualDemonstrationInterfacer;
import tracks.tutorialGeneration.ITSetParserGenerator.Graph;
import video.basics.BunchOfGames;
import video.basics.Interaction;
import core.logging.Logger;

public class TutorialGenerator extends AbstractTutorialGenerator{
	private LevelAnalyzer la;
	private GameAnalyzer ga;
	private GameDescription game;
	private String gameFile;
	
	private String[] agents = {"adrienctx.Agent", "NovelTS.Agent", "NovTea.Agent", "Number27.Agent", "YOLOBOT.Agent"};
	
	private ArrayList<String> necessaryFrames;
	public TutorialGenerator(SLDescription sl, GameDescription game, ElapsedCpuTimer time, String gameFile) {
		la = new LevelAnalyzer(sl);
		ga = new GameAnalyzer(game);
		this.game = game;
		this.gameFile = gameFile;
		necessaryFrames = new ArrayList<String>();
	}
	
	/**
	 * Searches GameDescription.getAllSpriteData() for the given spritename
	 * @param spriteName the sprite you are looking for
	 * @return the sprite, if it exists
	 */
	private SpriteData searchForSprite(String spriteName) {
		// grab all sprites in the game
		ArrayList<SpriteData> allSpriteData = game.getAllSpriteData();
		// iterate over this list, return the sprite with the same name
		for(SpriteData me : allSpriteData) {
			if(me.name.equals(spriteName)) {
				return me;
			}
		}
		return null;
	}

	@Override
	public String[] generateTutorial(GameDescription game, SLDescription sl, ElapsedCpuTimer elapsedTimer) {
		String[] generatedTutorial = new String[0];
		ArrayList<String> generatedTutorialList = new ArrayList<String>();
		
		// Build Graph of interactions
		GraphBuilder graph = new GraphBuilder(game, sl, ga, la);
		graph.buildGraph();
		
		/**
		 * Path information
		 */
		ArrayList<String> controls = getControlsInformation(graph);
		ArrayList<String> winPathString = traceCriticalPaths(graph);
		ArrayList<String> pointsPathString = findPoints(graph);
		ArrayList<String> losePathString = findLosses(graph);
		
		ArrayList<Mechanic> winPath = graph.winPath;
		ArrayList<Mechanic> losePath = graph.losePath();
		ArrayList<Mechanic> pointsPath = graph.pointsPath();
		// viz tutorial
		createVisualTutorial(winPath, pointsPath, losePath, controls, graph, winPathString, losePathString, pointsPathString);
		
		// written tutorial
		generatedTutorialList.addAll(controls);
		generatedTutorialList.addAll(winPathString);
		generatedTutorialList.addAll(pointsPathString);
		generatedTutorialList.addAll(losePathString);
		
		System.out.println("\n\nStart Tutorial File");
		for(String instruction : generatedTutorialList) {
			System.out.println(instruction);
		}
		return generatedTutorialList.toArray(generatedTutorial);
	}
	
	public ArrayList<String> traceCriticalPaths(GraphBuilder graph) {
		ArrayList<String> pathText = new ArrayList<String>();
		for(Entity terminal : graph.victoryTerminations) {
			ArrayList<String> longestPath = new ArrayList<String>();
			for(Entity av : graph.getAvatarEntites()) {
				
				// find longest path for avatar to win
				ArrayList<String> temp = graph.traceUserInteractionChain(av, terminal);
				if(temp.size() > longestPath.size()) {
					longestPath = temp;
				}

			}
			String tip = "This is how you win:";
			pathText.add(tip);
			pathText.addAll(longestPath);
			pathText.add("");
		}
	
		return pathText;
	}
	
	public ArrayList<String> findPoints(GraphBuilder graph) {
		ArrayList<String> pathText = new ArrayList<String>();
		
		String tip = "This is how you get points:";
		pathText.add(tip);
		pathText.addAll(graph.scoreChangers());
		pathText.add("");
		
		return pathText;
	}
	
	public ArrayList<String> findLosses(GraphBuilder graph) {
		ArrayList<String> pathText = new ArrayList<String>();
		
		String tip = "This is how you lose:";
		pathText.add(tip);
		pathText.addAll(graph.lossConditions());
		pathText.add("");
		
		return pathText;
	}
	
	public ArrayList<String> getControlsInformation(GraphBuilder graph) {
		ArrayList<Entity> avatars = graph.getAvatarEntites();
		ArrayList<String> movementTutorialList = new ArrayList<String>();
		String movementTutorial = "";
		String avatarType = "";
//		ArrayList<String> avatarTypes = new ArrayList<String>();
//		for(Entity avatar : avatars) {
//			avatarType = avatar.getSubtype();
//			if(!avatarTypes.contains(avatarType)) {
//				avatarTypes.add(avatarType);
//			}
//			
//		}
		String avatarParent = "";
		Entity avatar = graph.getAvatarEntites().get(0);
		
		if(avatar.getParents().size() > 0) {
			avatarParent = avatar.getParents().get(avatar.getParents().size() - 1);
		}
		else {
			avatarParent = avatar.getSubtype();
		}
		avatarType = avatar.getSubtype();
//		for(String at : avatarTypes) {
//			avatarType = at;
		movementTutorial += "To move as the " + avatarParent + ",";
		if(avatarType.equals("MovingAvatar")){
			movementTutorial += " use the four arrow keys to move.";
		} else if(avatarType.equals("HorizontalAvatar") || avatarType.equals("FlakAvatar")) {
			movementTutorial += " use the left and right arrow keys to move.";
			// extra information needed for OngoingShoot, Shoot, and Flak avatars
			if(avatarType.equals("FlakAvatar")) {
				movementTutorial += extraMovementInformation(avatarType, avatars, graph);
			}
		} else if(avatarType.equals("VerticalAvatar")) {
			movementTutorial += " use the up and down arrow keys to move.";
		} else if(avatarType.equals("OngoingAvatar") || avatarType.equals("OngoingShootAvatar")) {
			// extra information needed for OngoingShoot, Shoot, and Flak avatars
			movementTutorial += " use the arrow keys to change direction.";
			if(avatarType.equals("OngoingShootAvatar")) {
				movementTutorial += extraMovementInformation(avatarType, avatars, graph);
			}
		} else if(avatarType.equals("OngoingTurningAvatar")) {
			// TODO : reword this better?
			movementTutorial += " use the arrow keys to change direction. You cannot do 180 degree turns!";
		} else if(avatarType.equals("MissileAvatar")) {
			// TODO : Figure out what this means
			movementTutorial += " unsure what this means...";
		} else if(avatarType.equals("OrientedAvatar") || avatarType.equals("ShootAvatar")) {
			movementTutorial += " use the arrow keys to turn and move.";
			// extra information needed for OngoingShoot, Shoot, and Flak avatars
			if(avatarType.equals("ShootAvatar")) {
				movementTutorial += extraMovementInformation(avatarType, avatars, graph);
			}

		} else {
			movementTutorial += " the system is unsure of what this means.";
		}
//		}
		movementTutorialList.add("This is how you control your avatar:");
		movementTutorialList.add(movementTutorial);
		movementTutorialList.add("");
		return movementTutorialList;
	}
		
	public String extraMovementInformation(String avatarType, ArrayList<Entity> avatars, GraphBuilder graph) {
		String extraMovementTutorial = "";
		Entity avatar = null;
		for(Entity e : avatars) {
			if(e.getSubtype().equals(avatarType)) {
				avatar = e;
				break;
			}
		}
		// find the thing the avatar is shooting	
		Pair missilePair = avatar.getAttribute("stype");
		String missileName = missilePair.getValue();
		
		Entity missileObject = graph.searchObjects(missileName);
		String missileType = missileObject.getSubtype();
		if(missileType.equals("Missile")) {
			extraMovementTutorial += " Press space to shoot a " + missileObject.getFullName() + ".";
		} else if(missileType.equals("OrientedFlicker")) {
			extraMovementTutorial += " Press space to use the " + missileObject.getFullName() + ".";
		} else if(missileType.equals("Immovable")) {
			extraMovementTutorial += " Press space to release a " + missileObject.getFullName() + ".";
		}
		return extraMovementTutorial;
	}
	
	
	
	/***
	 * Generates a visual tutorial JSON file
	 * @param winPath
	 * @param pointsPath
	 * @param losePath
	 * @param controls
	 */
	public void createVisualTutorial(ArrayList<Mechanic> winPath, ArrayList<Mechanic> pointsPath, ArrayList<Mechanic> losePath, ArrayList<String> controls, GraphBuilder graph, ArrayList<String> winText, ArrayList<String> loseText, ArrayList<String> pointsText) {
		try {

			
			// Writes the game info to JSON
			writeGameInfo(controls, graph);
			// the visual demonstrator which creates frames
			VisualDemonstrationInterfacer vdi = new VisualDemonstrationInterfacer();
			String levelFile = "";
			
			
			ArrayList<BunchOfGames> bogs = new ArrayList<>();
			/** Add games for all agents on all levels **/
			for(int i = 0; i < agents.length; i++) {
				for(int j = 0; j < 5; j++) {
					levelFile = gameFile.replace(".txt", "_lvl" + j + ".txt");
					bogs.add(new BunchOfGames(gameFile, levelFile, agents[i]));
				}
			}
//			vdi.runGame(gameFile, levelFile, "tracks.singlePlayer.advanced.olets.Agent");
			vdi.runBunchOfGames(bogs);

			// Writes the win info to JSON
			writeWinInfo(winPath, graph, vdi, winText);
			writeLoseInfo(losePath, graph, vdi, loseText);
			writePointsInfo(pointsPath, graph, vdi, pointsText);
			
			// gets rid of all the trash frames we don't need
			throwAwayTrash();
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void throwAwayTrash() {
		try{
			final File folder = new File("frames");
			for (final File fileEntry : folder.listFiles()) {
				if(!necessaryFrames.contains("frames/" + fileEntry.getName())) {
					fileEntry.delete();
				}
		        
		    }
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	public void writeGameInfo(ArrayList<String> controls, GraphBuilder graph) {
		String gameName = gameFile.replace(".txt", "");
		gameName = gameName.substring(gameName.indexOf('/')+1);
		gameName = gameName.substring(gameName.indexOf('/')+1);
		try (FileWriter file = new FileWriter("queriedFrames/" + gameName + "_visTutorial.json")) {
			// start and game info writing
			String stuffToWrite = "{\n\t\"gameInfo\" : {\n";

			stuffToWrite += "\t\t\"gameName\"\t\t: \"" + gameName + "\",\n";
			String parent = "";
			if(graph.getAvatarEntites().get(0).getParents().size() > 0) {
				parent = graph.getAvatarEntites().get(0).getParents().get(graph.getAvatarEntites().get(0).getParents().size() - 1);
			}
			else {
				parent = graph.getAvatarEntites().get(0).getSubtype();
			}
			stuffToWrite += "\t\t\"avatarInfo\"\t: \"You are the " + parent + ".\",\n";
			stuffToWrite += "\t\t\"controlsInfo\"\t: \"";
			for(int i = 1; i < controls.size(); i++) {
				String control = controls.get(i);
				stuffToWrite += control;
			}
			stuffToWrite += "\",\n";
			stuffToWrite += "\t\t\"avatarImage\"\t: \"" + game.getAvatar().get(0).parameters.get("img") + "\"";
			stuffToWrite += "\n\t},";
			
//			stuffToWrite += game.getAvatar().get(0).parameters.get("img");
			file.write(stuffToWrite);
			file.flush();
			file.close();

		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public void writeWinInfo(ArrayList<Mechanic> winPath, GraphBuilder graph, VisualDemonstrationInterfacer vdi, ArrayList<String> winText) {
		String gameName = gameFile.replace(".txt", "");
		gameName = gameName.substring(gameName.indexOf('/')+1);
		gameName = gameName.substring(gameName.indexOf('/')+1);
		ArrayList<ArrayList<Mechanic>> superP = graph.visualPathGeneralization(winPath);
		
		try (FileWriter file = new FileWriter("queriedFrames/" + gameName + "_visTutorial.json", true)) {
			String stuffToWrite = "\n\t\"winRules\" : [\n";

			
			// Writes the win path to JSON
			for(int i = 0; i < winPath.size()-1; i++) {
				Mechanic win = winPath.get(i);
				stuffToWrite += (i != 0 ? "," : "") + "\n\t\t{\"text\" : \t\"" + graph.getInteractionString(win, 1) + "\"";
				int index = 0;
				try{
					String[] frames = vdi.retrieveFramePaths(win.getAction().getName(), win.getObject1().getName(), win.getObject2().getName());
					for(int j = 0; j < frames.length; j++) {
						stuffToWrite += ", \"image" + j + "\" : \"" + frames[j] + "\"";
						necessaryFrames.add(frames[j]);
						index++;
					}
					// add all frames to this guy
				} catch(Exception e) {
					stuffToWrite += ", \"image" + 0 + "\" : \"" + "bah" + "\", \"image" + 1 + "\" : \"" + "bah" + "\",  \"image" + 2 + "\" : \"" + "bah" + "\"";
					index++;
					e.printStackTrace();
				}
				stuffToWrite += "}";
			}
			
			stuffToWrite += "\n\t],";
			file.write(stuffToWrite);
			file.flush();
			file.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void writeLoseInfo(ArrayList<Mechanic> losePath, GraphBuilder graph, VisualDemonstrationInterfacer vdi, ArrayList<String> loseText) {
		String gameName = gameFile.replace(".txt", "");
		gameName = gameName.substring(gameName.indexOf('/')+1);
		gameName = gameName.substring(gameName.indexOf('/')+1);
		ArrayList<ArrayList<Mechanic>> superP = graph.visualPathGeneralization(losePath);

		try (FileWriter file = new FileWriter("queriedFrames/" + gameName + "_visTutorial.json", true)) {
			String stuffToWrite = "\n\t\"loseRules\" : [\n";

			
			// Writes the win path to JSON
			for(int i = 0; i < losePath.size(); i++) {
				Mechanic win = losePath.get(i);
				int index = 0;
				stuffToWrite += (i != 0 ? "," : "") + "\n\t\t{\"text\" : \t\"" + graph.getInteractionString(win, 1) + "\"";
				try{
					String[] frames = vdi.retrieveFramePaths(win.getAction().getName(), win.getObject1().getName(), win.getObject2().getName());
					for(int j = 0; j < frames.length; j++) {
						stuffToWrite += ", \"image" + j + "\" : \"" + frames[j] + "\"";
						necessaryFrames.add(frames[j]);
						index++;
					}
					// add all frames to this guy
				} catch(Exception e) {
					stuffToWrite += ", \"image" + 0 + "\" : \"" + "bah" + "\", \"image" + 1 + "\" : \"" + "bah" + "\",  \"image" + 2 + "\" : \"" + "bah" + "\"";
					index++;
					e.printStackTrace();
				}
				stuffToWrite += "}";
			}
			
			stuffToWrite += "\n\t],";
			file.write(stuffToWrite);
			file.flush();
			file.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void writePointsInfo(ArrayList<Mechanic> pointsPath, GraphBuilder graph, VisualDemonstrationInterfacer vdi, ArrayList<String> pointsText) {
		String gameName = gameFile.replace(".txt", "");
		gameName = gameName.substring(gameName.indexOf('/')+1);
		gameName = gameName.substring(gameName.indexOf('/')+1);
		ArrayList<ArrayList<Mechanic>> superP = graph.visualPathGeneralization(pointsPath);

		try (FileWriter file = new FileWriter("queriedFrames/" + gameName + "_visTutorial.json", true)) {
			String stuffToWrite = "\n\t\"pointsRules\" : [\n";

			
			// Writes the win path to JSON
			for(int i = 0; i < superP.size(); i++) {
				Mechanic win = superP.get(i).get(0);
				stuffToWrite += (i != 0 ? "," : "") + "\n\t\t{\"text\" : \t\"" + graph.getInteractionString(win, 2) + "\"";
				int index = 0;
				
				// need to get the frames for each interaction after the 0 index for this superP list
				ArrayList<Mechanic> mechsToSearch = new ArrayList<Mechanic>();
				if(superP.get(i).size() > 1) {
					for(int k = 1; k < superP.get(i).size(); k++) {
						mechsToSearch.add(superP.get(i).get(k));
					}
				} else {
					mechsToSearch.add(win);
				}
				int counter = 0;
				stuffToWrite += ", \"images\":[";
				for(int k = 0; k < mechsToSearch.size(); k++) {
					Mechanic mech = mechsToSearch.get(k);
					try{
						
	//					for(int j = 0; j < super)
						String[] frames = vdi.mapFramePathsInTheCollectionByInteraction(new Interaction(mech.getObject1().getName(), mech.getObject2().getName(), mech.getAction().getOutputs().get(0).getName()));
//						String[] frames = vdi.retrieveFramePaths(win.getAction().getName(), win.getObject1().getName(), win.getObject2().getName());
						if(frames != null) {
							for(int j = 0; j < frames.length; j++) {
								stuffToWrite += frames[j];
								necessaryFrames.add(frames[j]);
								if(k != mechsToSearch.size() - 1 && j != frames.length - 1) {
									stuffToWrite += ", ";
								}
								index++;
								
							}
						}
						else {
							
						}
						// add all frames to this guy
					} catch(Exception e) {
//						stuffToWrite += ", \"image" + 0 + "\" : \"" + "bah" + "\", \"image" + 1 + "\" : \"" + "bah" + "\",  \"image" + 2 + "\" : \"" + "bah" + "\"";
						index++;
						e.printStackTrace();
					}
				}
				stuffToWrite += "]}";

			}
			
			stuffToWrite += "\n\t]\n}";
			file.write(stuffToWrite);
			file.flush();
			file.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
