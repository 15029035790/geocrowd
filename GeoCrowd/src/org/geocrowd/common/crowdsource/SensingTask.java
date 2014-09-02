/**
 * *****************************************************************************
 * @ Year 2013 This is the source code of the following papers.
 * 
* 1) Geocrowd: A Server-Assigned Crowdsourcing Framework. Hien To, Leyla
 * Kazemi, Cyrus Shahabi.
 * 
*
 * Please contact the author Hien To, ubriela@gmail.com if you have any
 * question.
 * 
* Contributors: Hien To - initial implementation
******************************************************************************
 */
package org.geocrowd.common.crowdsource;

import java.util.Random;
import org.geocrowd.common.Constants;

// TODO: Auto-generated Javadoc
/**
 * The Class SensingTask.
 *
 * @author HT186011
 *
 * Each task has a region (e.g., circle whose center is task location), in which
 * any worker within the region can perform the task
 */
public class SensingTask extends GenericTask {

    /**
     * The radius.
     */
    private double radius;		// of the task region

    /**
     * the required number of assigned workers
     */
    private int k;

    /**
     * Instantiates a new sensing task.
     *
     * @param radius the radius
     */
    public SensingTask(double radius) {
        this.radius = radius;
    }

    /**
     * Instantiates a new sensing task.
     *
     * @param lt the lt
     * @param ln the ln
     * @param entry the entry
     * @param ent the ent
     */
    public SensingTask(double lt, double ln, int entry, double ent) {
        super(lt, ln, entry, ent);
    }

    /**
     * Gets the radius.
     *
     * @return the radius
     */
    public double getRadius() {
        return radius;
    }

    /**
     * Sets the radius.
     *
     * @param radius the new radius
     */
    public void setRadius(double radius) {
        this.radius = radius;
    }

    /**
     *
     * @param k
     */
    public void setK(int k) {
        if (Constants.IS_RANDOM_K) {
            this.k = k;
        } else {
            Random r = new Random();
            this.k = r.nextInt(k) + 1;
        }

    }

    /**
     * Gets the K.
     *
     * @return the k
     */
    public int getK() {
        return k;
    }

}
