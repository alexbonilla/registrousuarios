/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iveloper.concesiones.consultasweb.beans;

import com.iveloper.concesiones.consultasweb.entities.AltaUsuarios;
import com.iveloper.concesiones.consultasweb.jsf.AltaUsuariosController;
import com.iveloper.concesiones.consultasweb.jsf.util.JsfUtil;
import com.iveloper.concesiones.consultasweb.jsf.util.JsfUtil.PersistAction;
import com.iveloper.concesiones.controllers.AltaUsuariosJpaController;
import com.microtripit.mandrillapp.lutung.MandrillApi;
import com.microtripit.mandrillapp.lutung.model.MandrillApiError;
import com.microtripit.mandrillapp.lutung.view.MandrillMessage;
import com.microtripit.mandrillapp.lutung.view.MandrillMessageStatus;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.UserTransaction;

/**
 *
 * @author alexbonilla
 */
@ManagedBean(name = "newUserBean")
@SessionScoped
public class NewUserBean implements Serializable {

    @EJB
    private com.iveloper.concesiones.consultasweb.beans.AltaUsuariosFacade ejbFacade;
    private String usuario;
    private String clave;
    private String id;
    private String nombre;
    private String email;
    private String telefono;
    private AltaUsuarios newUser;

    public AltaUsuarios getNewUser() {
        return newUser;
    }

    public void setNewUser(AltaUsuarios newUser) {
        this.newUser = newUser;
    }

    private AltaUsuariosFacade getFacade() {
        return ejbFacade;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public AltaUsuarios prepareCreate() {
        newUser = new AltaUsuarios();
        newUser.setUsuario(usuario);
        newUser.setClave(clave);
        newUser.setId(id);
        newUser.setNombre(nombre);
        newUser.setEmail(email);
        newUser.setTelefono(telefono);
        newUser.setAlta(Boolean.FALSE);
        newUser.setFechacreacion(new Date());
        return newUser;
    }

    public String create() {
        String result = "failure";
        AltaUsuarios nuevousuario = getFacade().find(prepareCreate().getUsuario());
        if (nuevousuario != null) {
            Logger.getLogger(NewUserBean.class.getName()).log(Level.INFO, "Usuario {0} ya existe", usuario);
            JsfUtil.addSuccessMessage("Usuario " + usuario + " ya existe");
        } else {
            persist(PersistAction.CREATE, ResourceBundle.getBundle("/Bundle").getString("AltaUsuariosCreated"));
            //avisar por correo a administrador de nueva solicitud de alta
            nuevousuario = getFacade().find(newUser.getUsuario());
            if (nuevousuario != null) {
                try {
                    MandrillApi mandrillApi = new MandrillApi("Wb30-AONNEEZioNKxX9BXQ");
                    //Se notifica a Admin
                    List<AltaUsuarios> administrators = null;
                    try {
                        EntityManagerFactory emf = Persistence.createEntityManagerFactory("registrousuariosPU");
                        Context c = new InitialContext();
                        UserTransaction utx = (UserTransaction) c.lookup("java:comp/UserTransaction");

                        AltaUsuariosJpaController accountController = new AltaUsuariosJpaController(utx, emf);
                        administrators = accountController.findAdmins();

                    } catch (NamingException ex) {
                        Logger.getLogger(AltaUsuariosController.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (Exception ex) {
                        Logger.getLogger(AltaUsuariosController.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if (administrators != null) {
                        // create your message
                        MandrillMessage message = new MandrillMessage();
                        message.setSubject("Consultas Web Concesiones");
                        message.setHtml("<p>Se ha solicitado el alta para el usuario " + nuevousuario.getUsuario() + ". Por favor proceder con la validaci&oacute;n de datos en <a href='http://concesiones.rastreototal.com:20004/altausuarios/faces/login.xhtml' target='_blank'>http://concesiones.rastreototal.com:20004/altausuarios/</a>.</p>");
                        message.setAutoText(true);
                        message.setFromEmail("alex@iveloper.com");
                        message.setFromName("Consultas Web Concesiones");
                        // add recipients                                        
                        ArrayList<MandrillMessage.Recipient> recipients = new ArrayList<MandrillMessage.Recipient>();
                        MandrillMessage.Recipient recipient = null;
                        Iterator<AltaUsuarios> administratorsItr = administrators.iterator();
                        while (administratorsItr.hasNext()) {
                            recipient =  new MandrillMessage.Recipient();
                            AltaUsuarios thisAdmin = administratorsItr.next();
                            recipient.setEmail(thisAdmin.getEmail());
                            recipient.setName(thisAdmin.getNombre());
                            recipients.add(recipient);
                        }

                        message.setTo(recipients);
                        message.setPreserveRecipients(true);
                        ArrayList<String> tags = new ArrayList<String>();
                        tags.add("notificacion");
                        tags.add("admin");
                        message.setTags(tags);

                        // send
                        MandrillMessageStatus[] messageStatusReports = mandrillApi.messages().send(message, false);
                    }
                    
                    //Se notifica a Usuario
                    MandrillMessage messageUser = new MandrillMessage();
                    messageUser.setSubject("ConsultasWeb Suite");
                    messageUser.setHtml("<p>Estimado " + nuevousuario.getNombre() +  ", su solicitud de alta ha sido recibida. Usted recibir&aacute; una respuesta de aprobaci&oacute;n en un lapso de 24 horas, luego de lo cual podr&aacute; acceder al sistema de Consultas Web.</p><p>Muchas gracias por utilizar nuestro servicios.</p>");
                    messageUser.setAutoText(true);
                    messageUser.setFromEmail("alex@iveloper.com");
                    messageUser.setFromName("ConsultasWeb Suite");
                    // add recipients
                    ArrayList<MandrillMessage.Recipient> recipientsUser = new ArrayList<MandrillMessage.Recipient>();
                    MandrillMessage.Recipient recipientUser = new MandrillMessage.Recipient();
                    recipientUser.setEmail(nuevousuario.getEmail());
                    recipientUser.setName(nuevousuario.getNombre());
                    recipientsUser.add(recipientUser);

                    messageUser.setTo(recipientsUser);
                    messageUser.setPreserveRecipients(true);
                    ArrayList<String> tagsUser = new ArrayList<String>();
                    tagsUser.add("notificacion");
                    tagsUser.add("user");
                    messageUser.setTags(tagsUser);
                    // ... add more message details if you want to!
                    // then ... send
                    MandrillMessageStatus[] messageStatusReportsUser = mandrillApi.messages().send(messageUser, false);
                    //ivalidate newUser
                    result = "success";
                } catch (MandrillApiError | IOException ex) {
                    Logger.getLogger(NewUserBean.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        usuario = "";
        clave = "";
        id = "";
        nombre = "";
        email = "";
        telefono = "";        
        return result;
    }

    private void persist(PersistAction persistAction, String successMessage) {

        if (newUser != null) {
            try {
                if (persistAction != PersistAction.DELETE) {
                    getFacade().edit(newUser);
                } else {
                    getFacade().remove(newUser);
                }
                JsfUtil.addSuccessMessage(successMessage);
            } catch (EJBException ex) {
                String msg = "";
                Throwable cause = ex.getCause();
                if (cause != null) {
                    msg = cause.getLocalizedMessage();
                }
                if (msg.length() > 0) {
                    JsfUtil.addErrorMessage(msg);
                } else {
                    JsfUtil.addErrorMessage(ex, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
                }
            } catch (Exception ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                JsfUtil.addErrorMessage(ex, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
            }
        }
    }

}
