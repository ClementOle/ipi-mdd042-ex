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
import java.util.*;
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
		List<String> listEmpInString = stream.sorted(Collections.reverseOrder()).collect(Collectors.toList());

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
		String car = ligne.substring(0, 1);
		switch (car) {
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
				throw new BatchException("Type d'employé inconnu : " + car);

		}
	}

	/**
	 * Méthode qui crée un Commercial à partir d'une ligne contenant les informations d'un commercial et l'ajoute dans la liste globale des employés
	 *
	 * @param ligneCommercial la ligne contenant les infos du commercial à intégrer
	 * @throws BatchException s'il y a un problème sur cette ligne
	 */
	private void processCommercial(String ligneCommercial) throws BatchException {
		List<String> list = new ArrayList<String>(Arrays.asList(ligneCommercial.split(",")));
		if (list.size() == NB_CHAMPS_COMMERCIAL) {
			Commercial commercial = new Commercial();

			commercial = (Commercial) setBasicData(commercial, list);

			commercial = (Commercial) setDate(commercial, list.get(3));

			commercial = (Commercial) setSalaire(commercial, list.get(4));


			try {
				commercial.setCaAnnuel(Double.parseDouble(list.get(5)));
			} catch (Exception e) {
				throw new BatchException("Le chiffre d'affaire du commercial est incorrect : " + list.get(5));
			}
			try {
				commercial.setPerformance(Integer.parseInt(list.get(6)));
			} catch (Exception e) {
				throw new BatchException("La performence du commercial est incorrect : " + list.get(6));
			}
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
		List<String> list = new ArrayList<String>(Arrays.asList(ligneManager.split(",")));
		if (list.size() == NB_CHAMPS_MANAGER) {
			Manager manager = new Manager();
			manager = (Manager) setBasicData(manager, list);

			manager = (Manager) setDate(manager, list.get(3));

			manager = (Manager) setSalaire(manager, list.get(4));

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
		List<String> list = new ArrayList<String>(Arrays.asList(ligneTechnicien.split(",")));
		if (list.size() == NB_CHAMPS_TECHNICIEN) {
			Technicien technicien = new Technicien();
			technicien = (Technicien) setBasicData(technicien, list);

			//Date//
			technicien = (Technicien) setDate(technicien, list.get(3));

			try {
				technicien.setGrade(Integer.parseInt(list.get(5)));
			} catch (TechnicienException te) {
				throw new BatchException("Le grade doit être compris entre 1 et 5 : " + list.get(5) + ", technicien");
			} catch (Exception e) {
				throw new BatchException("Le grade du technicien est incorrect : " + list.get(5));
			}

			technicien = (Technicien) setSalaire(technicien, list.get(4));

			if (list.get(6).matches(REGEX_MATRICULE_MANAGER)) {
				boolean found = false;
				for (Employe employe : employes) {
					if (employe.getMatricule().equals(list.get(6))) {
						technicien.setManager((Manager) employe);
						found = true;
					}
				}
				if (!found)
					throw new BatchException("Le manager de matricule " + list.get(6) + " n'a pas été trouvé dans le fichier ou en base de données");
			} else {
				throw new BatchException("La châine " + list.get(6) + " ne respecte pas l'expression regulière " + REGEX_MATRICULE_MANAGER);
			}

			employes.add(technicien);
		} else {
			throw new BatchException("La ligne technicien ne contient pas " + NB_CHAMPS_TECHNICIEN + " éléments mais " + list.size());
		}

	}

	/**
	 *
	 * @param employe
	 * @param list
	 * @return
	 * @throws BatchException
	 */
	private Employe setBasicData(Employe employe, List<String> list) throws BatchException {
		if (list.get(0).matches(REGEX_MATRICULE))
			employe.setMatricule(list.get(0));
		else
			throw new BatchException("la chaîne " + list.get(0) + " ne respecte pas l'expression régulière " + REGEX_MATRICULE);
		if (list.get(1).matches(REGEX_NOM))
			employe.setNom(list.get(1));
		else
			throw new BatchException("la chaîne " + list.get(0) + " ne respecte pas l'expression régulière " + REGEX_NOM);
		if (list.get(2).matches(REGEX_PRENOM))
			employe.setPrenom(list.get(2));
		else
			throw new BatchException("la chaîne " + list.get(0) + " ne respecte pas l'expression régulière " + REGEX_PRENOM);
		return employe;
	}

	/**
	 *
	 * @param employe
	 * @param date
	 * @return
	 * @throws BatchException
	 */
	private Employe setDate(Employe employe, String date) throws BatchException {
		try {
			LocalDate dateFormater = LocalDate.parse(date, DateTimeFormat.forPattern("dd/MM/YYYY"));
			employe.setDateEmbauche(dateFormater);
		} catch (Exception e) {
			throw new BatchException(date + " ne respecte pas le format dd/MM/yyyy");
		}
		return employe;
	}

	private Employe setSalaire(Employe employe, String salaire) throws BatchException {
		try {
			employe.setSalaire(Double.parseDouble(salaire));
		} catch (Exception e) {
			throw new BatchException(salaire + " n'est pas un nombre valide pour un salaire");
		}
		return employe;
	}
}
