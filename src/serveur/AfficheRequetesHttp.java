package serveur;

import java.io.*;

import java.net.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Scanner;

public class AfficheRequetesHttp {
	
	// TODO: multithreader le serveur pour que chaque requête soit traitée dans un thread différent

	/* ServerSocket s sert a ecouter les connexions entrantes. Socket c : la connexion avec un client particulier
	 * Pour multithreader, il faut surement (thread.start()) à chaque fois que (Socket c = s.accept()) 
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
					String firstline = sc.nextLine();
					System.out.println(firstline);
					// lire la première ligne sur flux d'entrée
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
						String requestedFile = Paths.get(args[1]).resolve(Paths.get(firstline.split(" ")[1])).normalize().toAbsolutePath().toString();
						System.out.println("REQUESTED FILE : "+ requestedFile);// quand je veux "GET /bin", ça print \bin.
						//si je mets toAbsolutePath, donne C:\bin au lieu de donner l'adresse à partir du serveur local
						
						/*
						Path cheminServeur = Paths.get(args[1]).toAbsolutePath().normalize(); 
						System.out.println("CHEMIN SERVEUR : "+cheminServeur); //pour l'instant c'est args[0] == "."
						// prints "CHEMIN SERVEUR : C:\Users\honopio\git\serveurJava\."
						
						String cheminComplet = cheminServeur.resolve(requestedFile).toString();
						System.out.println("CHEMIN COMPLET : " + cheminComplet);
						// prints "CHEMIN COMPLET : C:\bin" instead of C:\Users\honopio\git\serveurJava\bin

						/* requestedFile = la ressource demandée dans le navigateur (par ex, "/bin")
						 * par contre, ce qui est chargé ds le browser est le contenu de args[1], pas requestedFile
						 * quand on demande "http://localhost:1080/bin", la requête est "GET /bin HTTP/1.1" 
						 * MAIS on rentre PAS DANS LE IF(directory) 
						 */
						
						
						// TODO: SI (CHEMIN est un répertoire) ALORS
						if (Files.isDirectory(Paths.get(requestedFile))) {
							//dir à partir duquel le serveur envoie des files, combiné au fichier de la requête
							Path chemin = Paths.get(requestedFile); 
							
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
							
							//obtenir le path du directory parent
						    Path parent = chemin.getParent();
						    System.out.println("PARENT DIRECTORY : "+ parent);

							pw.println("<tr><td valign=\"top\"></td><td><a href=\""+ parent + "\">Parent Directory</a></td>");
						    pw.println("</tr>");
						    
							//afficher tous les fichiers du dir
							try (DirectoryStream<Path> stream = Files.newDirectoryStream(chemin)) {
								
							    for (Path file: stream) {
							    	//classe qui permet d'accéder à des méthodes qui renvoient des attributs de files
							    	BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
							    	
							    	pw.println("<tr><td valign=\"top\"></td><td><a href=\"" + file.getFileName() + "\">" + file.getFileName() 
							    	+ "</a><td align=\"right\">"+  attrs.lastModifiedTime().toString() +"</td><td align=\"right\">" + attrs.size() + " bytes" + "</td></td></tr>");
							    }
							    
							pw.println("</table>");    
							pw.println("</body></html>");
							pw.flush(); // il faut vider le buffer pour que le contenu soit envoyé.
							}
						}/*
						// TODO: SINON SI (CHEMIN est un fichier)
						else if (Files.isRegularFile(chemin)) {
							// construire réponse avec le contenu du fichier
							// exemple d'envoi de fichier
							String mimeType = Files.probeContentType(chemin);
							// Infos sur types MIME = https://developer.mozilla.org/fr/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types
							Writer w = new OutputStreamWriter(os);
							PrintWriter pw = new PrintWriter(w);
							pw.println("HTTP/1.1 200 OK");
							pw.println("Content-Type: " + mimeType);
							pw.println();// ligne vide pour signaler la fin des entêtes
							pw.flush();// il faut vider le buffer pour que le contenu soit envoyé. envoie à OutputStream os je crois
		
							// Copier le contenu d'un ficher vers un flux en sortie
							Files.copy(chemin, os); // ici pas besoin de vider le buffer os n'est pas buffeurisé
						}*/
						else {
						// TODO: SINON
							// envoyer erreur 404
							Writer w = new OutputStreamWriter(os);
							PrintWriter pw = new PrintWriter(w);
							pw.println("HTTP/1.1 404 DEAD");
							pw.flush(); // il faut vider le buffer pour que le contenu soit envoyé.
						}
						// FIN SI
					}
					// TODO: fermer la connexion. la classe ServerSocket a une methode close(). c.close(); */
					// FIN TANT QUE
					
				}

			}
		} catch (IOException e) {
			System.err.println("IO error: ");
			e.printStackTrace();
			System.exit(10);
		}

	}
}