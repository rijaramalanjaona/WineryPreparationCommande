package com.winery.listener;

import java.util.List;
import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ingesup.winery.ejb.commande.RemoteCommandeManager;
import com.ingesup.winery.ejb.detail.RemoteDetailCommandeManager;
import com.ingesup.winery.ejb.produit.RemoteProduitManager;
import com.ingesup.winery.entity.Commande;
import com.ingesup.winery.entity.DetailCommande;
import com.ingesup.winery.entity.Produit;

public class CommandeListener implements MessageListener {

    public static void main(String[] args) {
	try {
	    Properties props = new Properties();
	    props.setProperty("java.naming.provider.url", "localhost:1099");
	    props.setProperty("java.naming.factory.initial",
		    "org.jnp.interfaces.NamingContextFactory");
	    props.setProperty("java.naming.factory.url.pkgs",
		    "org.jboss.naming:org.jnp.interfaces");

	    QueueConnection connection;
	    QueueSession session;
	    MessageConsumer consumer;
	    CommandeListener commandeListener = new CommandeListener();

	    Queue file;

	    // configuer le contexte
	    Context ctx = new InitialContext(props);

	    // 1 creer une connection
	    QueueConnectionFactory facto = (QueueConnectionFactory) ctx
		    .lookup("ConnectionFactory");
	    connection = facto.createQueueConnection();

	    // 2 creer une session
	    session = connection.createQueueSession(false,
		    Session.AUTO_ACKNOWLEDGE);

	    // 3 creer client
	    file = (Queue) ctx.lookup("/queue/wineryCmd");
	    consumer = session.createConsumer(file);
	    consumer.setMessageListener(commandeListener);
	    connection.start();
	    while (true) {
	    }
	} catch (JMSException e) {
	    e.printStackTrace();
	} catch (NamingException e) {
	    e.printStackTrace();
	}
    }

    @Override
    public void onMessage(Message msg) {
	if (msg instanceof ObjectMessage) {
	    System.out
		    .println("------------ Traitement de la commande recue ------------");

	    Context ctx;

	    Properties props = new Properties();
	    props.setProperty("java.naming.provider.url", "localhost:1099");
	    props.setProperty("java.naming.factory.initial",
		    "org.jnp.interfaces.NamingContextFactory");
	    props.setProperty("java.naming.factory.url.pkgs",
		    "org.jboss.naming:org.jnp.interfaces");

	    try {
		ctx = new InitialContext(props);
		RemoteProduitManager produitManager = (RemoteProduitManager) ctx
			.lookup(RemoteProduitManager.JNDI_NAME);

		RemoteCommandeManager commandeManager = (RemoteCommandeManager) ctx
			.lookup(RemoteCommandeManager.JNDI_NAME);

		RemoteDetailCommandeManager detailCommandeManager = (RemoteDetailCommandeManager) ctx
			.lookup(RemoteDetailCommandeManager.JNDI_NAME);

		Commande commande = (Commande) ((ObjectMessage) msg)
			.getObject();
		commande = commandeManager.getById(commande.getId());
		System.out.println("Id commande : " + commande.getId());

		List<DetailCommande> listeDetailCommande = detailCommandeManager
			.getByIdCommande(commande.getId());
		if (listeDetailCommande != null
			&& !listeDetailCommande.isEmpty()) {
		    boolean commandeValide = true;
		    System.out.println("Details commande : ");
		    for (DetailCommande detailCommande : listeDetailCommande) {
			Produit produit = detailCommande.getProduit();
			System.out.println("idDetailCommande : " + detailCommande.getId()
				+ " idProduit : " + produit.getId()
				+ " qte commandee : " + detailCommande.getQuantite() 
				+ " stock : " + produit.getStock());

			// test si quantite commandee < quantite en stock
			if (produit.getStock() < detailCommande.getQuantite()) {
			    commandeValide = false;
			    System.out
				    .println("Quantite commandee pour le produit "
					    + produit.getId()
					    + " : "
					    + detailCommande.getQuantite()
					    + " > stock : "
					    + produit.getStock());
			    System.out.println("Commande annulee");
			    commande.setEtat("Annulee");
			    commandeManager.update(commande);
			    break;
			}
		    }

		    if (commandeValide) {
			System.out.println("Mise a jour stock");
			for (DetailCommande detailCommande : listeDetailCommande) {
			    Produit produit = detailCommande.getProduit();
			    Long resteStock = produit.getStock()
				    - detailCommande.getQuantite();
			    produit.setStock(resteStock);
			    produitManager.update(produit);
			}

			System.out.println("Commande traitee");
			commande.setEtat("Traitee");
			commandeManager.update(commande);
		    }
		}

	    } catch (NamingException e) {
		e.printStackTrace();
	    } catch (JMSException e) {
		e.printStackTrace();
	    }

	}

    }

}
