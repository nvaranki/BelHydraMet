/*
 * Weather.java
 * Created on April 18, 2006, 7:58 PM
 * Copyright 2006 Nikolai Varankine. All rights reserved.
 *
 * This class implements main view.
 */

package com.varankin.BelHydraMeteoCenter;

import com.varankin.mobile.Dispatcher;
import com.varankin.mobile.http.*;
import com.varankin.mobile.util.*;
import java.util.*;
import javax.microedition.lcdui.*;
import javax.microedition.io.*;

/**
 * @author  Nikolai Varankine
 */
public class Weather extends Canvas implements CommandListener, HttpLinker
{
    private final static String PROP_SERVER = "Server"; // URL
    private Command CMD_INQUIRE, CMD_EXIT, CMD_HELP, CMD_BACK;
    protected Application parent;
    private Image image;
    private List city_list = null;
    private String server;
    
    /** Creates a new instance of Weather */
    public Weather( Application a_parent ) 
    {
        super();
        parent = a_parent;
        CMD_INQUIRE = new Command( parent.getString(this,"Menu.Run"), Command.OK, 1 );
        CMD_EXIT = new Command( parent.getString(this,"Menu.Exit"), Command.BACK, 1 );
        CMD_HELP = new Command( parent.getString(this,"Menu.Help"), Command.SCREEN, 9 );
        CMD_BACK = new Command( parent.getString(this,"Menu.Back"), Command.BACK, 1 );
        server = HttpLink.PROTOCOL + parent.getAppProperty( PROP_SERVER );
        // prepare list of cities
        String[] names = new String[ parent.cities.length ];
        for( int c = 0; c < parent.cities.length; c ++ ) 
            names[c] = parent.getString( "Application.City." + 
                Integer.toString( parent.cities[c] ) + ".Name" );
        city_list = new List( parent.getString( this, "Cities" ), List.IMPLICIT, names, null );
        city_list.setCommandListener( this );
        // complete GUI
        image = parent.forecast; repaint();
        addCommand( CMD_INQUIRE );
        addCommand( CMD_EXIT );
        //addCommand( CMD_SETUP );
        //addCommand( CMD_HELP );
        switch( parent.getHelpMode() )
        {
        case Dispatcher.HELP_TICKER:
            // start dynamic help
            //parent.setTicker( new Ticker( parent.getString(this,"Ticker") ) );
            break;
        case Dispatcher.HELP_COMMAND:
            // start static help
            addCommand( CMD_HELP );
            break;
        }
        setCommandListener( this );
    }
    
    public void inquire( Alert a_message )
    {
        parent.city = Math.min( Math.max( 0, parent.city ), parent.cities.length-1 );
        String title = parent.getString( "Application.City." + 
                        Integer.toString( parent.cities[parent.city] ) + ".Name" );
        Item page = getHeight() > 128 ? (Item)new ImageItem( title, parent.logo, 
            ImageItem.LAYOUT_CENTER | ImageItem.LAYOUT_NEWLINE_AFTER, null ) 
            : (Item)new StringItem( null, title );
        Hashtable acc = new Hashtable(); acc.put( "Accept", "image/*" );
        if( parent.isLicenseValid() )
        {
            String expiration = parent.registry.getValue( Dispatcher.RKEY_EXPIRES );
            Alert notification = new Alert( null, parent.getString( "Registar.Expires" ) + 
                Dispatcher.formatDateTime( expiration ), null, AlertType.WARNING );
            if( a_message == null && parent.registry.getValue( Dispatcher.RKEY_INIT ) == null )
                a_message = notification;
            parent.setCurrent( a_message, new HttpLinkMonitor( HttpConnection.GET, server
                + Integer.toString( parent.cities[parent.city] ) + parent.type 
                + parent.size + parent.views[parent.view] + ".gif", null, acc, page, this ) );
        }
        else
            parent.setCurrent( AlertType.ALARM, 3, new Registar( parent, this, server ), null );
    }

    public void getCity()
    {
        parent.setCurrent( city_list );
    }

    public void commandAction( Command a_command, Displayable a_displayable ) 
    {
        if( a_displayable == city_list )
        {
            if( a_command == List.SELECT_COMMAND ) // accept selection
            {
                int old_city = parent.city;
                // get new city selection
                parent.city = city_list.getSelectedIndex();
                try { parent.registry.setValue( parent.RKEY_CITY, 
                        String.valueOf( parent.city ) ); }
                catch( Exception e ) {}
                // notify and return, inquire if city really changed
                Alert notification = new Alert( null, parent.getString( this, 
                    "Alert.6.Message" ) + city_list.getString( parent.city ), 
                    null, AlertType.CONFIRMATION );
                notification.setTimeout( 1*1000 ); // standard one is too long
                if( old_city != parent.city ) 
                {
                    image = parent.logo; 
                    if( parent.isAuto() ) inquire( notification );
                    else parent.setCurrent( notification, this );
                }
                else parent.setCurrent( notification, this );
            }

            else if( a_command == CMD_BACK ) // ignore selection of city
            {
                parent.setCurrent( this );
            }
        }
        else if( image == parent.schema )
        {
            if( a_command == CMD_BACK ) // ignore selection of scheme
            {
                image = parent.forecast; repaint();
                addCommand( CMD_INQUIRE );
                addCommand( CMD_EXIT );
                removeCommand( CMD_BACK );
            }
        }
        else if( a_command == CMD_EXIT ) parent.exitRequested();

        else if( a_command == CMD_INQUIRE ) inquire( null );

        else if( a_command == CMD_HELP ) 
        {
            Alert help = new Alert( null, parent.getString(this,"Ticker"), 
                null, AlertType.INFO );
            help.setTimeout( Alert.FOREVER );
            parent.setCurrent( help );
        }
        
    }

    private void selectColorScheme( int a_index )
    {
        int old_view = parent.view;
        // get new view selection
        parent.view = a_index - 1;
        try { parent.registry.setValue( parent.RKEY_VIEW, 
                String.valueOf( parent.view ) ); }
        catch( Exception e ) {}
        // restore GUI
        image = parent.forecast; repaint();
        addCommand( CMD_INQUIRE );
        addCommand( CMD_EXIT );
        removeCommand( CMD_BACK );
        // create sample image of selection
        int w3 = parent.schema.getWidth()/3, h3 = parent.schema.getHeight()/3;
        Image sel = Image.createImage( w3, h3 );
        sel.getGraphics().drawImage( parent.schema, -(parent.view % 3)*w3, -(parent.view / 3)*h3, 0 );
        Alert notification = new Alert( null, parent.getString( this, "Alert.3.Message" ) 
            + Integer.toString( a_index ), Image.createImage( sel ), 
                AlertType.CONFIRMATION );
        // notify and return, inquire if view really changed
        if( old_view != parent.view )
        {
            image = parent.logo; 
            if( parent.isAuto() ) inquire( notification );
            else parent.setCurrent( notification, this );
        }
        else parent.setCurrent( notification, this );
    }
    
    protected void keyPressed( int a_keyCode )
    {
        String keyName = getKeyName( a_keyCode );
        
        if( image == parent.schema ) switch( a_keyCode )
        {
            case KEY_NUM1: selectColorScheme( 1 ); break;
            case KEY_NUM2: selectColorScheme( 2 ); break;
            case KEY_NUM3: selectColorScheme( 3 ); break;
            case KEY_NUM4: selectColorScheme( 4 ); break;
            case KEY_NUM5: selectColorScheme( 5 ); break;
            case KEY_NUM6: selectColorScheme( 6 ); break;
            case KEY_NUM7: selectColorScheme( 7 ); break;
            case KEY_NUM8: selectColorScheme( 8 ); break;
            case KEY_NUM9: selectColorScheme( 9 ); break;
            case KEY_POUND:  // exit color scheme setup
                removeCommand( CMD_BACK );
                addCommand( CMD_INQUIRE );
                addCommand( CMD_EXIT );
                image = parent.forecast; repaint();
                break;
            default: break; // play sound here
        }

        else if( a_keyCode == KEY_POUND ) //|| a_keyCode == -4 || keyName.compareTo( "RIGHT" ) == 0 )
        {
            // enter color setup mode
            removeCommand( CMD_INQUIRE );
            removeCommand( CMD_EXIT );
            addCommand( CMD_BACK );
            image = parent.schema; repaint();
        }

        else if( a_keyCode == KEY_NUM0 ) //|| a_keyCode == -2 || keyName.compareTo( "DOWN" ) == 0 )
        {
            // toggle auto-request feature
            try { parent.setAuto( !parent.isAuto() ); } catch( Exception e ) {}
            parent.setCurrent( new Alert( null, parent.getString( this, 
                parent.isAuto() ? "Alert.4.Message" : "Alert.5.Message" ), 
                null, AlertType.CONFIRMATION ) );
        }

        else if( a_keyCode == KEY_STAR ) //|| a_keyCode == -3 || keyName.compareTo( "LEFT" ) == 0 )
        {
            if( city_list != null )
            {
                // select city
                city_list.addCommand( CMD_BACK );
                parent.city = Math.min( Math.max( 0, parent.city ), city_list.size()-1 );
                city_list.setSelectedIndex( parent.city, true );
                parent.setCurrent( city_list );
            }
        }

        else if( a_keyCode == KEY_NUM5 ) //|| a_keyCode == -1 || keyName.compareTo( "UP" ) == 0 )
        {
            parent.setCurrent( new Registar( parent, this, server ) );
        }

        else
            ; // play sound here
    }
    
    protected void paint( Graphics g )
    {
        // clear screen
        int widthScreen = getWidth(), heightScreen = getHeight();
        int widthImage = image.getWidth(), heightImage = image.getHeight();
        g.setColor( 0xFFFFFF ); // white
        g.fillRect( 0, 0, widthScreen, heightScreen );

        // draw centered image and wallpaper
        int xAnchor = ( widthScreen  - widthImage  )/2;
        int yAnchor = ( heightScreen - heightImage )/2;
        int xRepeat = xAnchor > 0 ? (xAnchor+widthImage/2 )/widthImage  : 0;
        int yRepeat = xAnchor > 0 ? (yAnchor+heightImage/2)/heightImage : 0;
        for( int r = -yRepeat; r <= yRepeat; r++ ) for( int c = -xRepeat; c <= xRepeat; c++ )
            if( r == 0 && c == 0 )
                g.drawImage( image, xAnchor, yAnchor, 0 );
            else
                g.drawImage( parent.logo, xAnchor+c*widthImage, yAnchor+r*heightImage, 0 );
    }
    
    public void completed( byte[] a_reply, int a_reply_size, HttpConnection a_conn )
    {
        try 
        { 
            // repaint new image off-screen
            String content_type = a_conn.getType();
            if( content_type != null && !content_type.startsWith( "image/" ) )
                throw new IllegalArgumentException( content_type );
            else try 
            { 
                parent.forecast = Image.createImage( a_reply, 0, a_reply_size ); 
                image = parent.forecast;
            }
            catch( IllegalArgumentException e ) 
            { 
                throw new IllegalArgumentException( content_type ); 
            }
            repaint(); 

            // determine expiration date
            long modified = 0L;
            try { modified = HttpLink.getLastModified( a_conn ); }
            catch( Exception e ) { modified = 0L; }
            parent.expired = Long.parseLong( parent.getAppProperty( 
                    modified == 0L ? "Latency" : "Validity" ) )*60*60*1000; // 1 or 24 hour(s)
            if( modified == 0L ) modified = (new Date()).getTime();
            parent.expired += modified;

            // remember image and its expiration date
            try
            {
                parent.registry.setValue( parent.RKEY_IMAGE, a_reply );
                parent.registry.setValue( parent.RKEY_IMAGE_EXPIRES, String.valueOf( parent.expired ) );
            }
            catch( Exception e ) {}

            // notify and display new image on screen
            parent.setCurrent( new Alert( null, parent.getString( this, "Alert.1.Message" ) 
                + Dispatcher.formatDateTime( modified ), null, AlertType.CONFIRMATION ), this );
        }
        catch( Exception e ) { parent.setCurrent( AlertType.ERROR, 2, this, e ); }
    }

    public void interrupted( Exception a_problem ) 
    {
        parent.setCurrent( AlertType.ERROR, 7, this, a_problem );
    }

    public Dispatcher getDispatcher()
    {
        return parent;
    }

}
