package serveur;

import java.io.*;

import java.net.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Scanner;

public class AfficheRequetesHttp {
	
	// TODO: multithreader le serveur pour que chaque requête soit traitée dans un thread différent

	 /* Pour multithreader, il faut surement (thread.start()) à chaque fois que (Socket c = s.accept()) 
	  * pour fermer la connexion : fermer le socket (c.close()) et tuer le thread. see : https://stackoverflow.com/questions/44989876/simple-java-multi-threaded-socket-application
	  */
	
	public static void main(String[] args) throws NoSuchFileException {
		if (args.length != 2) {
			System.err.println("Usage: java " + AfficheRequetesHttp.class.getName() + "portnumber directory");
			System.exit(1);
		}
		//arg[1] doit etre un directory 
		Path dir = Paths.get(args[1]);
		if (!(Files.isDirectory(dir))) {
			System.err.println(dir +" isn't a directory");
			System.exit(2);
		}
		//dir doit pas remonter plus loin que le dir du serveur
		if (!(dir.toAbsolutePath().startsWith(Paths.get("").toAbsolutePath()))) {
			System.err.println(dir + " isn't inside the server directory");
			System.exit(3);
		}

		int portNumber = 0;
		try {
			portNumber = Integer.parseInt(args[0]);
			if (portNumber < 0) {
				System.err.println(args[0] + " is not a positive integer");
			}
		} catch (NumberFormatException e) {
			System.err.println(args[0] + " is not an integer");
			System.err.println("Usage: java " + AfficheRequetesHttp.class.getName() + " portnumber directory");
			System.exit(4);
		}
		// écouter sur port portNumber
		try (ServerSocket s = new ServerSocket(portNumber);) {
			// TANT QUE (vrai) FAIRE
			while (true) {
				// accepter connexion
				try (Socket c = s.accept();) {
					// récupérer flux d'entrée
					Scanner sc = new Scanner(c.getInputStream());
					System.out.println("----DEBUT REQUETE----");
					// stocker la premiere ligne pour type de requête
					//if ferme a la fin. sinon NoSuchElementException levee au bout de quelques secondes pcq le scanner continue d'essayer de lire
					if (sc.hasNextLine()) { 
				        String firstline = sc.nextLine();
				        System.out.println(firstline);
					
					// lire a partir de la deuxieme ligne sur flux d'entrée
					while (sc.hasNextLine()) {
						String line = sc.nextLine();
						// on sort de la boucle si on arrive sur la ligne vide de fin des headers
						if ("".equals(line)) {
							break;
						}
						System.out.println("-" + line + "-");
					}
					System.out.println("----FIN REQUETE----");
					
					// récupérer flux de sortie
					OutputStream os = c.getOutputStream();
					
					// SI (méthode get) ALORS
					if (firstline.substring(0, 3).equals("GET")) {
					
						// récupérer le chemin dans la première ligne
						String requestedFile = (firstline.split(" ")[1]);
						Path requestedFilePath = Paths.get(requestedFile);
												
						// "GET /" renvoie le contenu du dir serveurJava
					    if (requestedFile.equals("/")) {
					        requestedFilePath = Paths.get("");
					    } else { //sinon on concatene le chemin de serveurJava avec le chemin de la requete GET
					        requestedFilePath = Paths.get("").resolve(requestedFile.substring(1));
					    }
		/*TRACE*/		System.out.println("\nREQUESTED FILE : "+ requestedFile);

						// SI (CHEMIN est un répertoire) ALORS
						if (Files.isDirectory(requestedFilePath)) {
							
							// reponse affichant le contenu du répertoire
							
							Writer w = new OutputStreamWriter(os);
							PrintWriter pw = new PrintWriter(w);
							pw.println("HTTP/1.1 200 OK");
							pw.println("Content-Type: text/html");
							pw.println();// ligne vide pour signaler la fin des entêtes 
							
							//CORPS DE LA REQUETE : (sera le code source de la ressource envoyee)
							pw.println("<!DOCTYPE html PUBLIC \"-//IETF//DTD HTML 2.0//EN\">");
							pw.println("<html><body>");
							pw.println("<h1>Index of " + requestedFile + "</h1>");
							pw.println("<table>");
							pw.println("<tr><th valign=\"top\"></th><th><a>Name</a></th><th><a>Last modified</a></th><th><a>Size</a></th></tr>");
							
							// Récupération de l'adresse du Parent Directory
							String ParentRequestedFile = "";
							int derniersSlash = requestedFile.lastIndexOf('/'); // Trouver le dernier '/'
							if (derniersSlash == requestedFile.indexOf('/')) { //s'il y a un seul slash dans le chemin -> on veut remonter au dir racine
								ParentRequestedFile = requestedFile.substring(0, derniersSlash+1); //on inclut le slash dans le chemin
							} else if (derniersSlash != -1) { // Si / est trouvé
							    ParentRequestedFile = requestedFile.substring(0, derniersSlash);
         /*TRACE*/			System.out.println(derniersSlash);							
							} else
								ParentRequestedFile = requestedFile;

	
							//pb : quand on veut remonter au Parent Directory, on remonte directement au dir racine du projet.
							//pb v2: maintenant on remonte au Parent Directory mais pas au dessus.
							pw.println("<tr><td valign=\"top\"></td><td><a href=\"" + ParentRequestedFile + "\">Parent Directory</a></td>"); //le href est "/../"
						    pw.println("</tr>");
						    
							//afficher tous les fichiers du dir
							try (DirectoryStream<Path> stream = Files.newDirectoryStream(requestedFilePath)) {
								
							    for (Path file: stream) {
							    	//classe avec des methodes pour acceder à des attributs de fichiers
							    	BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
							    	
							    	//decide si la taille du fichier sera affichee en bits, KB ou MB
							    	long fileSize = attrs.size();  char format;
							    	if (fileSize < 1024) {
							    		format = 'B';
							    	} else if (fileSize < 1024*1024) {
							    		fileSize = attrs.size()/1024 ;
							    		format = 'K';
							    	} else {
							    		fileSize = attrs.size()/(1024*1024);
							    		format = 'M';
							    	}
							    	
							    	//pr chaque fichier, on print son nom (avec un href qui y envoie), sa date de derniere modif et sa taille
							    	pw.println("<tr><td valign=\"top\"></td><td><a href=\"" + requestedFile.substring(1) + "/" + file.getFileName() + "\">" + file.getFileName() 
							    	+ "</a><td align=\"right\">"+  attrs.lastModifiedTime().toString() +"</td><td align=\"right\">" + fileSize + format + "</td></td></tr>");
							    }
							    
							pw.println("</table>");    
							pw.println("</body></html>");
							pw.flush(); // il faut vider le buffer pour que le contenu soit envoyé.
							}
						}
						//SINON SI (CHEMIN est un fichier)
						else if (Files.isRegularFile(requestedFilePath)) {
					
							String mimeType = Files.probeContentType(requestedFilePath);
							// Infos sur types MIME = https://developer.mozilla.org/fr/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types
							Writer w = new OutputStreamWriter(os);
							PrintWriter pw = new PrintWriter(w);
							pw.println("HTTP/1.1 200 OK");
							pw.println("Content-Type: " + mimeType);
							pw.println();// ligne vide pour signaler la fin des entêtes
							pw.flush();// il faut vider le buffer pour que le contenu soit envoyé. envoie à OutputStream os je crois
		
							// Copier le contenu d'un ficher vers un flux en sortie
							Files.copy(requestedFilePath, os); // ici pas besoin de vider le buffer os n'est pas buffeurisé
						}
						else {
						//SINON (envoyer erreur 404)
							Writer w = new OutputStreamWriter(os);
							PrintWriter pw = new PrintWriter(w);
							pw.println("HTTP/1.1 404 DEAD");
							pw.flush(); // il faut vider le buffer pour que le contenu soit envoyé.
						}
						
					} //FIN SI(METHODE GET)
					}//FIN SI (il y a une ligne a lire)
		
				}//FIN try(accepter la connexion). 
				//connexion ouverte dans un try with resources, donc elle est fermee automatiquement ici 

			} //FIN while(true)
			
		} catch (IOException e) {
			System.err.println("IO error: ");
			e.printStackTrace();	
		}

	}
}