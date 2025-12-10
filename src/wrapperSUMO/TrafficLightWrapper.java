package wrapperSUMO;


import de.tudresden.sumo.cmd.Trafficlight;
import de.tudresden.sumo.cmd.Lane;
import it.polito.appeal.traci.SumoTraciConnection;
import java.util.List;
import java.util.ArrayList;


public class TrafficLightWrapper
{
    // initialize the connection
    private final SumoTraciConnection connection;

    // Constructor
    public TrafficLightWrapper(SumoTraciConnection connection)
    {
        this.connection = connection;
    }

    // get traffic light's IDs
    public List<String> getTrafficLightIDs()
    {
        try
        {
            return (List<String>) connection.do_job_get(Trafficlight.getIDList());
        }
        catch (Exception e)
        {
            System.out.println("Failed to get ID of Traffic Lights");
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    // get the number of traffic lights
    public int getTrafficLightCount()
    {
        try
        {
            return (int) connection.do_job_get(Trafficlight.getIDCount());
        }
        catch (Exception e)
        {
            System.out.println("Failed to get the number of Traffic Lights");
            e.printStackTrace();
        }
        return 0;
    }

    // get the traffic light's state
    public String getRedYellowGreenState(String trafficLightId)
    {
        try
        {
            return (String) connection.do_job_get(Trafficlight.getRedYellowGreenState(trafficLightId));
        }
        catch (Exception e)
        {
            System.out.println("Failed to get the state of Traffic Lights" + trafficLightId);
            e.printStackTrace();
        }
        return "0";
    }

    // get the duration of each phase
    public double getphaseduration(String trafficLightId)
    {
        try
        {
            return (double) connection.do_job_get(Trafficlight.getPhaseDuration(trafficLightId));
        }
        catch (Exception e)
        {
            System.out.println("Failed to get the duration of Traffic Lights");
        }
        return 0;
    }

    // get the current phase
    public int getcurrentphase(String trafficLightId)
    {
        try
        {
            return (int) connection.do_job_get(Trafficlight.getPhase(trafficLightId));
        }
        catch (Exception e)
        {
            System.out.println("Failed to get the duration of Traffic Lights");
        }
        return 0;
    }

    //set certain traffic light state
    public void settrafficlightstate(String trafficLightId, String phasestate)
    {
        try
        {
            connection.do_job_set(Trafficlight.setRedYellowGreenState(trafficLightId, phasestate));
        } catch (Exception e) {
            System.out.println("Failed to set the Traffic Lights" + trafficLightId);
        }
    }

    // set the manual phase
    public void setmanualphase_and_duration(String trafficLightId, String phase, double duration)
    {
        try
        {
            Trafficlight.setRedYellowGreenState(trafficLightId, phase);
            // set the phase duration manually
            connection.do_job_set(Trafficlight.setPhaseDuration(trafficLightId, duration));
            System.out.println(trafficLightId + " has been set to " + phase + "with duration " + duration);
        }
        catch (Exception e)
        {
            System.out.println("Failed to set the duration of Traffic Lights" + trafficLightId);
        }
    }

    // set automatic mode
    public void setautomaticmode(String trafficLightId)
    {
        try
        {
            connection.do_job_set(Trafficlight.setProgram(trafficLightId, "0"));
        }
        catch (Exception e)
        {
            System.out.println("Failed to set the program of Traffic Lights" + trafficLightId);
        }
    }

    // set manual mode with custom phase cycle
    public void set_manual_mode(String trafficLightId, double red_dur, double green_dur, double yellow_dur)
    {
        try
        {
            String current_state = (String) connection.do_job_get(Trafficlight.getRedYellowGreenState(trafficLightId));
            int number_of_lanes = current_state.length();
            connection.do_job_set(Trafficlight.setProgram(trafficLightId, "off"));
        }
        catch (Exception e)
        {
            System.out.println("Failed to set the program of Traffic Lights" + trafficLightId);
        }
    }
}