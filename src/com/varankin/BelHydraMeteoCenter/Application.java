/*
 * Application.java
 * Created on April 18, 2006, 7:29 PM
 * Copyright 2006 Nikolai Varankine. All rights reserved.
 *
 * This class implements main midlet.
 */

package com.varankin.BelHydraMeteoCenter;

import com.varankin.mobile.Dispatcher;
import com.varankin.mobile.http.*;
import java.io.*;
import java.lang.Long;
import java.util.*;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;
import javax.microedition.rms.*;

/**
 * @author  Nikolai Varankine
 */
public class Application extends Dispatcher 
{
    public  final static String RKEY_IMAGE = "Forecast.Image";
    public  final static String RKEY_IMAGE_EXPIRES = "Forecast.Expires";
    private final static String RKEY_AUTO = "Forecast.Auto";
    public  final static String RKEY_VIEW = "Forecast.View";
    public  final static String RKEY_CITY = "Forecast.City";
    public  final static int[] cities = 
    { 
        1, // Брест
        3, // Витебск
        5, // Гомель
        7, // Гродно
        9, // Могилев
        11 // Минск
    };
    public final static String type = "pr", size = "100x100";
    public final static String[] views = 
    {
        "b", // light blue
        "w", // white 
        "g", // light green
        "b-b", // guicy blue
        "y", // light yellow
        "b-bl", // dark blue
        "br", // brown
        "r", // red
        "o" // olive
    };

    public Image forecast = null, logo = null, schema = null;
    public int view = 3, city;
    public long expired;
    
    private boolean m_auto;
    
    /** Creates a new instance of Application */
    public Application() throws IOException, RecordStoreException 
    {
//#if PersonalEdition
//#         super( Long.MAX_VALUE >> 1 ); // unlimited
//#else
        super( 30*60*1000L ); // 365*24*1*60
//#endif
        // examine registry settings
        try 
        {
            if( registry.getValue( RKEY_VIEW ) == null )
                registry.setValue( RKEY_VIEW, "3" );
            view = Integer.parseInt( registry.getValue( RKEY_VIEW ) );

            String auto = registry.getValue( RKEY_AUTO );
            m_auto = auto == null || auto.compareTo( String.valueOf( true ) ) == 0;

            String cityr = registry.getValue( RKEY_CITY );
            city = cityr != null ? Integer.parseInt( cityr ) : -1;

            // setup default image
            String path = "/" + this.getClass().getName().replace( '.', '/' );
            path = path.substring( 0, path.lastIndexOf( '/' ) );
            try { logo   = Image.createImage( path + "/res/BelHydraMeteoCenter.png" ); }
            catch( java.io.IOException e ) { logo   = Image.createImage( 100, 100 ); }
            try { schema = Image.createImage( path + "/res/ColorMenu.png" ); }
            catch( java.io.IOException e ) { schema = Image.createImage( 100, 100 ); }
            forecast = logo; expired = 0L;

            // setup valid last known image for immediate view
            String image_expires = registry.getValue( RKEY_IMAGE_EXPIRES );
            if( image_expires != null ) expired = Long.parseLong( image_expires );
            if( !isExpired() )
            {
                byte[] last_image = registry.getBinaryValue( RKEY_IMAGE );
                if( last_image != null )
                    try{ forecast = Image.createImage( last_image, 0, last_image.length ); }
                    catch( Exception e ) { forecast = logo; }
                else
                    expired = 0L;
            }
        }
        catch( RecordStoreException e ) 
        { 
            exitRequested();
        }

        // complete important checks before start
        if( false && ! isMIDletValid( 43774 ) ) //DEBUG false && 
            // shame on you, hackers
            exitRequested();

        // start required form first
        Weather main_view = new Weather( this );
        if( city < 0 || city >= cities.length ) main_view.getCity(); // force selection
        else if( isAuto() || forecast == logo ) main_view.inquire( null ); // auto start
        else setCurrent( main_view ); // wait for user action
    }
    //public boolean isLicenseValid() { return true; } //DEBUG

    public boolean isExpired()
    {
        return Calendar.getInstance( TimeZone.getTimeZone( "GMT" ) ).getTime().getTime() > expired;
    }
    public final boolean isAuto()
    {
        return m_auto;
    }
    public final void setAuto( boolean a_new_mode ) throws RecordStoreException
    {
        m_auto = a_new_mode;
        registry.setValue( RKEY_AUTO, String.valueOf( m_auto ) );
    }

    public void startApp() 
    {
        setCurrent();
    }
    
    public void pauseApp() 
    {
    }
    
    public void destroyApp( boolean unconditional ) 
    {
    }

    public void exitRequested() 
    { 
        destroyApp(false); notifyDestroyed(); 
    }

}
