package com.iveloper.concesiones.consultasweb.jsf;

import com.iveloper.concesiones.consultasweb.entities.AltaUsuarios;
import com.iveloper.concesiones.consultasweb.jsf.util.JsfUtil;
import com.iveloper.concesiones.consultasweb.jsf.util.JsfUtil.PersistAction;
import com.iveloper.concesiones.consultasweb.beans.AltaUsuariosFacade;
import com.iveloper.concesiones.utils.SendFileEmail;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

@ManagedBean(name = "altaUsuariosController")
@SessionScoped
public class AltaUsuariosController implements Serializable {

    @EJB
    private com.iveloper.concesiones.consultasweb.beans.AltaUsuariosFacade ejbFacade;
    private List<AltaUsuarios> items = null;
    private AltaUsuarios selected;
    private Date now;

    public AltaUsuariosController() {
    }

    public AltaUsuarios getSelected() {
        return selected;
    }

    public void setSelected(AltaUsuarios selected) {
        this.selected = selected;
    }

    protected void setEmbeddableKeys() {
    }

    protected void initializeEmbeddableKey() {
    }

    private AltaUsuariosFacade getFacade() {
        return ejbFacade;
    }

    public AltaUsuarios prepareCreate() {
        selected = new AltaUsuarios();
        selected.setAlta(Boolean.FALSE);
        selected.setFechacreacion(new Date());
        initializeEmbeddableKey();
        return selected;
    }

    public void create() {

        AltaUsuarios usuario = getFacade().find(selected.getUsuario());
        if (usuario != null) {
            Logger.getLogger(AltaUsuariosController.class.getName()).log(Level.INFO, "Usuario ya existe");
        } else {
            persist(PersistAction.CREATE, ResourceBundle.getBundle("/Bundle").getString("AltaUsuariosCreated"));
            //avisar por correo a administrador de nueva solicitud de alta
            usuario = getFacade().find(selected.getUsuario());
            if (usuario != null) {
            //Se notifica a Admin
            SendFileEmail sfe = new SendFileEmail();
            sfe.setUser("invoicehub@iveloper.com");
            sfe.setPwd("bgNILL1982");
            sfe.setHost("gator4095.hostgator.com");
            sfe.setPort("465");
            sfe.setSsl("true");
            sfe.setAuth("false");
            sfe.setFrom("invoicehub@iveloper.com");
            sfe.setTo("info@concegua.com");
            sfe.setMessageSubject("ConsultasWeb Suite");
            sfe.setMessageBody("<p>Se ha solicitado el alta para el usuario " + usuario.getUsuario() + ". Por favor proceder con la validaci&oacute;n de datos.</p>");
            Thread sendMailThread = new Thread(sfe);
            sendMailThread.start();

                //Se notifica a Usuario
            SendFileEmail sfeUser = new SendFileEmail();
            sfeUser.setUser("invoicehub@iveloper.com");
            sfeUser.setPwd("bgNILL1982");
            sfeUser.setHost("gator4095.hostgator.com");
            sfeUser.setPort("465");
            sfeUser.setSsl("true");
            sfeUser.setAuth("false");
            sfeUser.setFrom("invoicehub@iveloper.com");
            sfeUser.setTo(usuario.getEmail());
            sfeUser.setMessageSubject("ConsultasWeb Suite");
            sfeUser.setMessageBody("<p>Su solicitud de alta ha sido recibida. Usted recibir&aacute; una respuesta de aprobaci&oacute;n en un lapso de 24 horas, luego de lo cual podr&aacute; acceder al sistema de Consultas Web.</p><p>Muchas gracias por utilizar nuestro servicios.</p>");
            Thread sendMailUserThread = new Thread(sfeUser);
            sendMailUserThread.start();
            }
        }
        if (!JsfUtil.isValidationFailed()) {
            items = null;    // Invalidate list of items to trigger re-query.
        }
    }

    public void update() {
        persist(PersistAction.UPDATE, ResourceBundle.getBundle("/Bundle").getString("AltaUsuariosUpdated"));
        //Se notifica a Usuario si fue dado de alta
        if (selected.getAlta()) {
            SendFileEmail sfeUser = new SendFileEmail();
            sfeUser.setUser("invoicehub@iveloper.com");
            sfeUser.setPwd("bgNILL1982");
            sfeUser.setHost("gator4095.hostgator.com");
            sfeUser.setPort("465");
            sfeUser.setSsl("true");
            sfeUser.setAuth("false");
            sfeUser.setFrom("invoicehub@iveloper.com");
            sfeUser.setTo(selected.getEmail());
            sfeUser.setMessageSubject("ConsultasWeb Suite");
            sfeUser.setMessageBody("<p>Su solicitud de alta ha sido aprobada. Le recordamos que su usario es " + selected.getUsuario() + ", ingrese utilizando su clave.</p>");
            Thread sendMailUserThread = new Thread(sfeUser);
            sendMailUserThread.start();
        }
    }

    public void destroy() {
        persist(PersistAction.DELETE, ResourceBundle.getBundle("/Bundle").getString("AltaUsuariosDeleted"));
        if (!JsfUtil.isValidationFailed()) {
            selected = null; // Remove selection
            items = null;    // Invalidate list of items to trigger re-query.
        }
    }

    public List<AltaUsuarios> getItems() {
        if (items == null) {
            items = getFacade().findAll();
        }
        return items;
    }

    private void persist(PersistAction persistAction, String successMessage) {

        if (selected != null) {
            setEmbeddableKeys();
            try {
                if (persistAction != PersistAction.DELETE) {
                    getFacade().edit(selected);
                } else {
                    getFacade().remove(selected);
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

    public List<AltaUsuarios> getItemsAvailableSelectMany() {
        return getFacade().findAll();
    }

    public List<AltaUsuarios> getItemsAvailableSelectOne() {
        return getFacade().findAll();
    }

    @FacesConverter(forClass = AltaUsuarios.class)
    public static class AltaUsuariosControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            AltaUsuariosController controller = (AltaUsuariosController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "altaUsuariosController");
            return controller.getFacade().find(getKey(value));
        }

        java.lang.String getKey(String value) {
            java.lang.String key;
            key = value;
            return key;
        }

        String getStringKey(java.lang.String value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof AltaUsuarios) {
                AltaUsuarios o = (AltaUsuarios) object;
                return getStringKey(o.getUsuario());
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "object {0} is of type {1}; expected type: {2}", new Object[]{object, object.getClass().getName(), AltaUsuarios.class.getName()});
                return null;
            }
        }

    }

}
