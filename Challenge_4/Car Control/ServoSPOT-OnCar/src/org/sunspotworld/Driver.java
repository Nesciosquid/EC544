/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sunspotworld;

/**
 *
 * @author Aaron Heuckroth
 */
public class Driver {
    public static void main(String[] args){
        IRDaemon d = new IRDaemon();
        double set_positions[] = new double[]{-30,-30,-30,-30,-30,-30,-30,-30,-30,-30,-30,-30,-30,-30,-30,-30};
        double current_positions[] = new double[]{-20,-22,-24,-26,-28,-30,-32,-34,-36,-38,-40,-42,-44,-46,-48,-50};        
       double current_thetas[] = new double[]{0,10,15,10,5,0,-8,-12,-16,-20,-20,-20,-20,-15,-10,0};
       for (int i=0; i<set_positions.length; i++){
           double set_dist = set_positions[i];
           double cur_dist = current_positions[i];
           double cur_thet = d.toRadians(current_thetas[i]);         
           double des_thet = d.calcIdealTheta(set_dist, cur_dist);
           int des_turn = d.calcTurn(des_thet, cur_thet);
           System.out.println("set_dist: " + set_dist + " cur_dist: " + cur_dist + " cur_thet " + cur_thet + " des_thet: " + des_thet + " des_turn: " + des_turn + " | angle_only: " + d.calcTurnFromAngle(cur_thet, 0.0) + " all in one turn:" + d.calcIdealTurn(set_dist, cur_dist, cur_thet));
       }
}
}

    