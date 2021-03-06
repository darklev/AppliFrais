package Passerelle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;

import Coeur.FicheFrais;
import Coeur.LigneFraisForfait;
import Coeur.LigneFraisHorsForfait;
import Coeur.Visiteur;


public class Passerelle 
{
	
	private Passerelle(){}
	
	/**
	 * Retourne toutes les fiches de frais sous forme d'ArrayList d'un Visiteur dont
	 * l'id est pass� en param�tre. 
	 * 
	 * @param id
	 * 		L'id du visiteur.
	 * @return Toutes les fiches de frais sous forme du Visiteur.
	 * @throws SQLException
	 */
	public static ArrayList<FicheFrais> get_fiches_frais_visiteur(String id) throws SQLException 
	{
		
		Connection c = Connexion.getConnexion();
		ArrayList<FicheFrais> lesFichesFrais = new ArrayList<FicheFrais>();
		String req = "SELECT FicheFrais.*, Etat.id as idEt, Etat.libelle as libEtat " +
					"FROM FicheFrais, Visiteur, Etat " +
					"WHERE FicheFrais.idVisiteur = Visiteur.id " +
					"AND Etat.id = FicheFrais.idEtat " +
					"AND idVisiteur = '"+id+"' "; 
		ResultSet res = c.createStatement().executeQuery(req);
		
		while(res.next())
		{
			String mois = res.getString("mois");
			int nbJustifs = res.getInt("nbJustificatifs");
			double montantValide = res.getDouble("montantValide");
			Date dateModif = res.getDate("dateModif");
			String idEtat = res.getString("idEt");
			String libEtat = res.getString("libEtat");			
			lesFichesFrais.add(new FicheFrais(mois,id,nbJustifs,montantValide,dateModif,idEtat,libEtat,get_lignes_frais_hors_forfait(id,mois),get_lignes_frais_forfait(id,mois)));
		}
		res.close();
		return lesFichesFrais;
	}
	
	/**
	 * Retourne toutes les lignes frais forfait d'une fiche de frais identifi� par
	 * l'identifiant du visiteur et le mois correspondant � la fiche.
	 * 
	 * @param idVisiteur
	 * 		Identifiant du visiteur � qui la fiche appartient.
	 * @param mois
	 * 		Mois coorespondant � la fiche.
	 * @return
	 * 		Une ArrayList de LigneFraisForfait correspondant � la fiche.
	 *  
	 * @throws SQLException
	 */
	private static ArrayList<LigneFraisForfait> get_lignes_frais_forfait(String idVisiteur, String mois) throws SQLException
	{
		ArrayList<LigneFraisForfait> listeLigneFraisForfait = new ArrayList <LigneFraisForfait>();	
	
		Connection conn = Connexion.getConnexion();
		Statement statement = conn.createStatement();
		String req = "SELECT * FROM Lignefraisforfait, fraisforfait " +
				"WHERE idVisiteur = '"+idVisiteur+"' "+
				"AND mois = '"+mois+"'" +
				"AND fraisforfait.id = idFraisForfait";
		ResultSet res = statement.executeQuery(req);
		
		while(res.next()) {
			LigneFraisForfait ligne = new LigneFraisForfait(res.getString("idFraisForfait"), res.getString("libelle"), res.getInt("quantite"), res.getDouble("montant"));
			listeLigneFraisForfait.add(ligne);
		}

		return listeLigneFraisForfait;
	}
	
	/**
	 * Retourne toutes les lignes frais hors forfait d'une fiche de frais identifi� par
	 * l'identifiant du visiteur et le mois correspondant � la fiche.
	 * 
	 * @param idVisiteur
	 * 		Identifiant du visiteur � qui la fiche appartient.
	 * @param mois
	 * 		Mois coorespondant � la fiche.
	 * @return
	 * 		Une ArrayList de LigneFraisHorsForfait "attach�" � la fiche.
	 *  
	 * @throws SQLException
	 */
	private static ArrayList<LigneFraisHorsForfait> get_lignes_frais_hors_forfait(String idVisiteur, String mois) throws SQLException
	{
		Connection conn = Connexion.getConnexion();
		ArrayList <LigneFraisHorsForfait> listeHorsForfait = new ArrayList <LigneFraisHorsForfait>();
		ResultSet res = null;


		Statement statement = conn.createStatement();
		String req = "SELECT * FROM lignefraishorsforfait " +
				"WHERE  idVisiteur = '"+idVisiteur+"'" +
				"AND mois = '" +mois+"' " +
				"order by id";
		res = statement.executeQuery(req);

		while(res.next()){
			//LigneFraisHorsForfait(int id, Date date, double montant, String libelle)
			LigneFraisHorsForfait unHorsForfait = new LigneFraisHorsForfait(res.getInt("id"), res.getDate("date"), res.getDouble("montant"), res.getString("libelle"));
			listeHorsForfait.add(unHorsForfait);				
		}
		
		res.close();
		statement.close();

		
		return listeHorsForfait;
	}

	/**
	 * Valide un fiche de frais en prenant compte des modifications apport�es.
	 * 
	 * @param ficheFrais 
	 * 		La fiche Frais � valider.
	 * @throws SQLException 
	 */
	public static void validerFicheFrais(FicheFrais ficheFrais) throws SQLException 
	{
		Connection c = Connexion.getConnexion();
		
		//Mise � jour des lignes frais forfait.
		for(LigneFraisForfait l : ficheFrais.getLignesFraisForfait())
		{
			String req = "UPDATE ligneFraisForfait " +
					"SET quantite = ? " +
					"WHERE idFraisForfait = ? " +
					"AND mois = ? " +
					"AND idVisiteur = ?";
			PreparedStatement ps = c.prepareStatement(req);
			
			ps.setInt(1, l.getQuantite());
			ps.setString(2, l.getIdFraisForfait());
			ps.setString(3,ficheFrais.getMois());
			ps.setString(4, ficheFrais.getIdVisiteur());
			ps.execute();
			ps.close();
		}
		
		//Mise � jour des lignes hors forfaits
		for(LigneFraisHorsForfait hf : ficheFrais.getLignesFraisHorsForfait())
		{
			String req = "UPDATE ligneFraisHorsForfait " +
					"SET date = ?, montant = ?, libelle = ? " +
					"WHERE id = ?";
			
			PreparedStatement ps = c.prepareStatement(req);
			
			ps.setDate(1, hf.getDate());
			ps.setDouble(2, hf.getMontant());
			ps.setString(3, hf.getLibelle());
			ps.setInt(4, hf.getId());
			
			System.out.println(ps);
			
			ps.execute();
			ps.close();
		}
		
		//Mise � jour de la Fiche de Frais		
		String req = "UPDATE FicheFrais " +
				"SET nbJustificatifs = ? , montantValide = ?, idEtat = 'VA' " +
				"WHERE mois = ? " +
				"AND idVisiteur = ? ";
		PreparedStatement ps = c.prepareStatement(req);
		
		ps.setInt(1, ficheFrais.getNbJustificatifs());
		ps.setDouble(2, ficheFrais.getMontantValide());
		ps.setString(3, ficheFrais.getMois());
		ps.setString(4, ficheFrais.getIdVisiteur());
		ps.close();		
	}
	
	/**
	 * Refuse une ligne frais hors forfait en modifiant son libelle.
	 * (rajoute "REFUSE:"
	 * @param id
	 * 		L'identifiant de la ligne hors forfait.
	 * @throws SQLException
	 */
	public static void refusserHorsForfait(int id) throws SQLException 
	{
		Connection conn = Connexion.getConnexion();
		ResultSet res = null;
		String libelleIni = null;
		//UPDATE lignefraishorsforfait` SET libelle = "REFUSE "+value WHERE lignefraishorsforfait.id = id
	
		Statement statement = conn.createStatement();
		String req = "SELECT libelle FROM lignefraishorsforfait WHERE id = "+id;
		res = statement.executeQuery(req);
	
		res.close();			
		statement.close();

	
		statement = conn.createStatement();
		req = "UPDATE lignefraishorsforfait SET libelle = concat(REFUSE, libelle) + , date = current_date WHERE lignefraishorsforfait.id = " + id;
		res = statement.executeQuery(req);
		res.close();			
		statement.close();
		
	}
	
	/**
	 * Retourne le nom du visiteur dont l'id est pass� en param�tre.
	 * 
	 * @param id
	 * 		L'id du visiteur.
	 * @return
	 * 		Nom du Visiteur
	 * @throws SQLException
	 */
	public static String getNomVisiteur(String id) throws SQLException
	{
		Connection c = Connexion.getConnexion();
		String req = "SELECT nom FROM visiteur " +
				"WHERE id = '"+id+"'";
		ResultSet res = c.createStatement().executeQuery(req);
		String nom = null;
		if(res.next())
			nom = res.getString(1);
		res.close();
		return nom;
	}
	
	
	/**
	 * Retourne le prenom du visiteur dont l'id est pass� en param�tre.
	 * 
	 * @param id
	 * 		L'id du visiteur.
	 * @return
	 * 		Prenom du Visiteur
	 * @throws SQLException
	 */
	public static String getPrenomVisiteur(String id) throws SQLException
	{
		Connection c = Connexion.getConnexion();
		String req = "SELECT prenom FROM visiteur " +
				"WHERE id = '"+id+"'";
		ResultSet res = c.createStatement().executeQuery(req);
		
		if(res.next())
			return res.getString(1);
		return null;
	}
	
	/**
	 * V�rifier le login et mdp pass�s en param�tre (existence dans
	 * la table compatble).
	 * 
	 * @param log
	 * 		Login du compatble.
	 * @param mdp
	 * 		Mot de passe du comptable.
	 * @return
	 * 	Retourne vrai si le couple login/mdp est valide (pr�sent dans la table comptable),
	 * sinon faux.
	 * @throws SQLException 
	 */
	public static boolean verifierConnexion(String log, String mdp) throws SQLException
	{
		Connection c = Connexion.getConnexion();
		
		PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM comptable " +
				"WHERE login = ? " +
				"AND mdp = ?");
		
		ps.setString(1, log);
		ps.setString(2, mdp);
		
		ResultSet rs = ps.executeQuery();
		
		rs.next();
		boolean ok = rs.getInt(1) != 0;
		ps.close();
		rs.close();
		return ok;
	}
	
	public static ArrayList<Visiteur> getLesVisiteurs() throws SQLException
	{
		Connection c = Connexion.getConnexion();
		String req = "SELECT * FROM visiteur";
		
		ResultSet res = c.createStatement().executeQuery(req);
		
		ArrayList<Visiteur> lesVisiteurs = new ArrayList<Visiteur>();
		
		while(res.next())
		{
			lesVisiteurs.add(new Visiteur(res.getString(1)));
			
		}
		
		return lesVisiteurs;
	}
}
