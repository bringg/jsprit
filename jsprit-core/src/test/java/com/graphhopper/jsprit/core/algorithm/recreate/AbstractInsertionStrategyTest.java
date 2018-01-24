package com.graphhopper.jsprit.core.algorithm.recreate;

import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import org.junit.Test;

import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractInsertionStrategyTest {
    final static Random RANDOM = new Random();

    @Test
    public void getSavings() {
        double fixedCostOld = RANDOM.nextInt(10) + RANDOM.nextDouble();
        double fixedCostNew = RANDOM.nextInt(10) + RANDOM.nextDouble();
        Vehicle oldVehicle = mock(Vehicle.class);
        when(oldVehicle.getType()).thenReturn(VehicleTypeImpl.Builder.newInstance(UUID.randomUUID().toString()).setFixedCost(fixedCostOld).build());

        Vehicle newVehicle = mock(Vehicle.class);
        when(newVehicle.getType()).thenReturn(VehicleTypeImpl.Builder.newInstance(UUID.randomUUID().toString()).setFixedCost(fixedCostNew).build());

        assertEquals(AbstractInsertionStrategy.getSavings(oldVehicle, newVehicle), fixedCostNew - fixedCostOld, .001);
    }

    @Test
    public void getSavingsSameFixedCost() {
        double fixedCost = RANDOM.nextInt(10) + RANDOM.nextDouble();
        Vehicle oldVehicle = mock(Vehicle.class);
        when(oldVehicle.getType()).thenReturn(VehicleTypeImpl.Builder.newInstance(UUID.randomUUID().toString()).setFixedCost(fixedCost).build());

        Vehicle newVehicle = mock(Vehicle.class);
        when(newVehicle.getType()).thenReturn(VehicleTypeImpl.Builder.newInstance(UUID.randomUUID().toString()).setFixedCost(fixedCost).build());

        assertEquals(AbstractInsertionStrategy.getSavings(oldVehicle, newVehicle), .0, .001);
    }
}
