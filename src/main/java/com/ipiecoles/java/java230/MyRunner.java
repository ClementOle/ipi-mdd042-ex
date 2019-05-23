package com.ipiecoles.java.java230;

import com.ipiecoles.java.java230.exceptions.BatchException;
import com.ipiecoles.java.java230.exceptions.TechnicienException;
import com.ipiecoles.java.java230.model.Commercial;
import com.ipiecoles.java.java230.model.Employe;
import com.ipiecoles.java.java230.model.Manager;
import com.ipiecoles.java.java230.model.Technicien;
import com.ipiecoles.java.java230.repository.EmployeRepository;
import com.ipiecoles.java.java230.repository.ManagerRepository;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class MyRunner implements CommandLineRunner {

	private static final String REGEX_MATRICULE = "^[MTC][0-9]{5}$";
	private static final String REGEX_NOM = ".*";
	private static final String REGEX_PRENOM = ".*";
	private static final int NB_CHAMPS_MANAGER = 5;
	private static final int NB_CHAMPS_TECHNICIEN = 7;
	private static final String REGEX_MATRICULE_MANAGER = "^M[0-9]{5}$";
	private static final int NB_CHAMPS_COMMERCIAL = 7;
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private EmployeRepository employeRepository;
	@Autowired
	private ManagerRepository managerRepository;
	private List<Employe> employes = new ArrayList<>();


	@Override
	public void run(String... strings) {
		String fileName = "employes.csv";
		readFile(fileName);
		//readFile(strings[0]);

	}

	/**
	 * Méthode qui lit le fichier CSV en paramètre afin d'intégrer son contenu en BDD
	 *
	 * @param fileName Le nom du fichier (à mettre dans src/main/resources)
	 * @return une liste contenant les employés à insérer en BDD ou null si le fichier n'a pas pu être le
	 */
	public List<Employe> readFile(String fileName) {
		Stream<String> stream;
		try {
			stream = Files.lines(Paths.get(new ClassPathResource(fileName).getURI()));
		} catch (IOException io) {
			logger.error("Problème dans l'ouverture du fichier " + fileName);
			return new ArrayList<>();
		}

		//Permet lors de l'ajout d'un manager à un technicien d'avoir traité tous les managers
		List<String> listEmpInString = stream.sorted().collect(Collectors.toList());

		logger.info(listEmpInString.size() + " lignes lues");
		//Traitement pour chaque ligne récupéré
		for (int i = 0; i < listEmpInString.size(); i++) {
			try {
				processLine(listEmpInString.get(i));
			} catch (BatchException b) {
				//Affichage du message d'erreur en spécifiant le numéro de ligne et le contenu de la ligne en erreur
				logger.error("Ligne " + i + " : " + b.getMessage() + " => " + listEmpInString.get(i));
			}
		}

		return employes;
	}

	/**
	 * Méthode qui regarde le premier caractère de la ligne et appelle la bonne méthode de création d'employé
	 *
	 * @param ligne la ligne à analyser
	 * @throws BatchException si le type d'employé n'a pas été reconnu
	 */
	private void processLine(String ligne) throws BatchException {
		//Traitement en fonction du type d'employé
		switch (ligne.substring(0, 1)) {
			case "T":
				processTechnicien(ligne);
				break;
			case "M":
				processManager(ligne);
				break;
			case "C":
				processCommercial(ligne);
				break;
			default:
				throw new BatchException("Type d'employé inconnu : " + ligne.substring(0, 1));

		}
	}

	/**
	 * Méthode qui crée un Commercial à partir d'une ligne contenant les informations d'un commercial et l'ajoute dans la liste globale des employés
	 *
	 * @param ligneCommercial la ligne contenant les infos du commercial à intégrer
	 * @throws BatchException s'il y a un problème sur cette ligne
	 */
	private void processCommercial(String ligneCommercial) throws BatchException {
		//Stockage de tous les éléments de la ligne dans une liste
		List<String> list = new ArrayList<String>(Arrays.asList(ligneCommercial.split(",")));
		//Si la list comprend le bon nombre d'éléments
		if (list.size() == NB_CHAMPS_COMMERCIAL) {
			Commercial commercial = new Commercial();
			//Assignation dans l'objet commercial de son matricule, nom et prénom
			commercial = (Commercial) setMatriculeNomPrenom(commercial, list);
			//Assignation dans l'objet commercial de sa date d'embauche
			commercial = (Commercial) setDateEmbauche(commercial, list.get(3));
			//Assignation dans l'objet commercial de son salaire
			commercial = (Commercial) setSalaire(commercial, list.get(4));

			//Assignation dans l'objet commercial de son chiffre d'affaire annuel
			try {
				commercial.setCaAnnuel(Double.parseDouble(list.get(5)));
			} catch (Exception e) {
				throw new BatchException("Le chiffre d'affaire du commercial est incorrect : " + list.get(5));
			}
			//Assignation dans l'objet commercial de sa performance
			try {
				commercial.setPerformance(Integer.parseInt(list.get(6)));
			} catch (Exception e) {
				throw new BatchException("La performence du commercial est incorrect : " + list.get(6));
			}
			//Ajout du commercial dans la liste des employés valide
			employes.add(commercial);

		} else {
			throw new BatchException("La ligne commercial ne contient pas " + NB_CHAMPS_COMMERCIAL + " éléments mais " + list.size());
		}
	}

	/**
	 * Méthode qui crée un Manager à partir d'une ligne contenant les informations d'un manager et l'ajoute dans la liste globale des employés
	 *
	 * @param ligneManager la ligne contenant les infos du manager à intégrer
	 * @throws BatchException s'il y a un problème sur cette ligne
	 */
	private void processManager(String ligneManager) throws BatchException {
		//Stockage de tous les éléments de la ligne dans une liste
		List<String> list = new ArrayList<String>(Arrays.asList(ligneManager.split(",")));
		//Si la list comprend le bon nombre d'éléments
		if (list.size() == NB_CHAMPS_MANAGER) {
			Manager manager = new Manager();
			//Assignation dans l'objet manager de son matricule, nom et prénom
			manager = (Manager) setMatriculeNomPrenom(manager, list);
			//Assignation dans l'objet manager de sa date d'embauche
			manager = (Manager) setDateEmbauche(manager, list.get(3));
			//Assignation dans l'objet manager de son salaire
			manager = (Manager) setSalaire(manager, list.get(4));

			//Ajout du manager dans la liste des employés valide
			employes.add(manager);
		} else {
			throw new BatchException("La ligne manager ne contient pas " + NB_CHAMPS_MANAGER + " éléments mais " + list.size());
		}

	}

	/**
	 * Méthode qui crée un Technicien à partir d'une ligne contenant les informations d'un technicien et l'ajoute dans la liste globale des employés
	 *
	 * @param ligneTechnicien la ligne contenant les infos du technicien à intégrer
	 * @throws BatchException s'il y a un problème sur cette ligne
	 */
	private void processTechnicien(String ligneTechnicien) throws BatchException {
		//Stockage de tous les éléments de la ligne dans une liste
		List<String> list = new ArrayList<String>(Arrays.asList(ligneTechnicien.split(",")));
		//Si la list comprend le bon nombre d'éléments
		if (list.size() == NB_CHAMPS_TECHNICIEN) {
			Technicien technicien = new Technicien();
			//Assignation dans l'objet technicien de son matricule, nom et prénom
			technicien = (Technicien) setMatriculeNomPrenom(technicien, list);

			//Assignation dans l'objet technicien de sa date d'embauche
			technicien = (Technicien) setDateEmbauche(technicien, list.get(3));

			//Assignation dans l'objet technicien de son matricule
			try {
				technicien.setGrade(Integer.parseInt(list.get(5)));
			} catch (TechnicienException te) {
				throw new BatchException("Le grade doit être compris entre 1 et 5 : " + list.get(5) + ", technicien");
			} catch (Exception e) {
				throw new BatchException("Le grade du technicien est incorrect : " + list.get(5));
			}
			//Assignation dans l'objet technicien de son salaire
			technicien = (Technicien) setSalaire(technicien, list.get(4));

			//Assignation dans l'objet technicien de son manager
			if (list.get(6).matches(REGEX_MATRICULE_MANAGER)) {
				//Traitement pour vérifier si le matricule est bien assigné à un manager
				boolean found = false;
				for (Employe employe : employes) {
					if (employe.getMatricule().equals(list.get(6))) {
						technicien.setManager((Manager) employe);
						found = true;
					}
				}
				//Si il n'existe pas on lance cette exception
				if (!found)
					throw new BatchException("Le manager de matricule " + list.get(6) + " n'a pas été trouvé dans le fichier ou en base de données");
			} else {
				throw new BatchException("La châine " + list.get(6) + " ne respecte pas l'expression regulière " + REGEX_MATRICULE_MANAGER);
			}
			//Ajout du technicien dans la liste des employés valide
			employes.add(technicien);
		} else {
			throw new BatchException("La ligne technicien ne contient pas " + NB_CHAMPS_TECHNICIEN + " éléments mais " + list.size());
		}

	}

	/**
	 * Méthode qui assigne à un employé passé en paramètre son matricule, nom et prénom contenu dans
	 * la liste passée en paramètre si les valeurs sont valide
	 *
	 * @param employe Employé traité
	 * @param list    des élément présente dans la ligne courante
	 * @return l'employé modifié si les valeurs était correct sinon renvoie de l'employé inchangé
	 * @throws BatchException si les valeurs de la liste ne sont pas valide
	 */
	private Employe setMatriculeNomPrenom(Employe employe, List<String> list) throws BatchException {
		//Assignation du matricule
		if (list.get(0).matches(REGEX_MATRICULE))
			employe.setMatricule(list.get(0));
		else
			throw new BatchException("la chaîne " + list.get(0) + " ne respecte pas l'expression régulière " + REGEX_MATRICULE);
		//Assignation du nom
		if (list.get(1).matches(REGEX_NOM))
			employe.setNom(list.get(1));
		else
			throw new BatchException("la chaîne " + list.get(0) + " ne respecte pas l'expression régulière " + REGEX_NOM);
		//Assignation du prénom
		if (list.get(2).matches(REGEX_PRENOM))
			employe.setPrenom(list.get(2));
		else
			throw new BatchException("la chaîne " + list.get(0) + " ne respecte pas l'expression régulière " + REGEX_PRENOM);
		//Renvoie de l'employé modifié ou non
		return employe;
	}

	/**
	 * Méthode qui assigne à l'employé passé en paramètre la date d'embauche passée en paramètre
	 *
	 * @param employe Employé traité
	 * @param date    Date d'embauche de l'employé en String
	 * @return l'employé modifié si la date passée en paramètre est valide
	 * @throws BatchException si la date n'est pas valide
	 */
	private Employe setDateEmbauche(Employe employe, String date) throws BatchException {
		try {
			//Conversion de la date de String à LocalDate
			LocalDate dateFormater = LocalDate.parse(date, DateTimeFormat.forPattern("dd/MM/YYYY"));
			//Assignation de la date d'embauche si aucune exception n'a été levé
			employe.setDateEmbauche(dateFormater);
		} catch (Exception e) {
			throw new BatchException(date + " ne respecte pas le format dd/MM/yyyy");
		}
		//Renvoie de l'employé modifié ou non
		return employe;
	}

	/**
	 * Méthode qui assigne à l'employé passé en paramètre le salaire passé en paramètre
	 *
	 * @param employe Employé traité
	 * @param salaire Salaire de l'employé en String
	 * @return l'employé modifié si le salaire passé en paramètre est valide
	 * @throws BatchException si le salaire n'est pas valide
	 */
	private Employe setSalaire(Employe employe, String salaire) throws BatchException {
		try {
			//Conversion du salaire de String à Double et assignation de se salaire à l'employé
			employe.setSalaire(Double.parseDouble(salaire));
		} catch (Exception e) {
			throw new BatchException(salaire + " n'est pas un nombre valide pour un salaire");
		}
		//Renvoie de l'employé modifié ou non
		return employe;
	}
}
