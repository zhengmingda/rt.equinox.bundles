/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.region.tests.system;

import org.eclipse.virgo.kernel.osgi.region.RegionFilterBuilder;

import org.osgi.framework.Constants;

import java.util.Map;

import java.util.HashMap;

import org.osgi.util.tracker.ServiceTracker;

import org.osgi.framework.InvalidSyntaxException;

import org.eclipse.virgo.kernel.osgi.region.RegionFilter;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.virgo.kernel.osgi.region.Region;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class RegionSystemTests extends AbstractRegionSystemTest {
	public void testBasic() throws BundleException, InvalidSyntaxException, InterruptedException{
		// get the system region
		Region systemRegion = digraph.getRegion(0);
		// create a disconnected test region
		Region testRegion = digraph.createRegion(getName());
		List<Bundle> bundles = new ArrayList<Bundle>();
		// Install all test bundles
		Bundle pp1, cp2, sc1;
		bundles.add(pp1 = testRegion.installBundle(bundleInstaller.getBundleLocation(PP1)));
		// should be able to start pp1 because it depends on nothing
		pp1.start();
		// do a sanity check that we have no services available in the isolated region
		assertNull("Found some services.", pp1.getBundleContext().getAllServiceReferences(null, null));
		assertEquals("Found extra bundles in region", 1, pp1.getBundleContext().getBundles().length);
		pp1.stop();

		bundles.add(testRegion.installBundle(bundleInstaller.getBundleLocation(SP1)));
		bundles.add(testRegion.installBundle(bundleInstaller.getBundleLocation(CP1)));
		bundles.add(testRegion.installBundle(bundleInstaller.getBundleLocation(PP2)));
		bundles.add(testRegion.installBundle(bundleInstaller.getBundleLocation(SP2)));
		bundles.add(cp2 = testRegion.installBundle(bundleInstaller.getBundleLocation(CP2)));	
		bundles.add(testRegion.installBundle(bundleInstaller.getBundleLocation(PC1)));
		bundles.add(testRegion.installBundle(bundleInstaller.getBundleLocation(BC1)));
		bundles.add(sc1 = testRegion.installBundle(bundleInstaller.getBundleLocation(SC1)));
		bundles.add(testRegion.installBundle(bundleInstaller.getBundleLocation(CC1)));

		// Import the system bundle from the systemRegion
		digraph.connect(testRegion, digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, "(id=0)").build(), systemRegion);
		// must import Boolean services into systemRegion to test
		digraph.connect(systemRegion, digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, "(objectClass=java.lang.Boolean)").build(), testRegion);

		bundleInstaller.resolveBundles(bundles.toArray(new Bundle[bundles.size()]));
		for (Bundle bundle : bundles) {
			assertEquals("Bundle did not resolve: " + bundle.getSymbolicName(), Bundle.RESOLVED, bundle.getState());
			bundle.start();
		}
		ServiceTracker<Boolean, Boolean> cp2Tracker = new ServiceTracker<Boolean, Boolean>(context, context.createFilter("(&(objectClass=java.lang.Boolean)(bundle.id=" + cp2.getBundleId() + "))"), null);
		ServiceTracker<Boolean, Boolean> sc1Tracker = new ServiceTracker<Boolean, Boolean>(context, context.createFilter("(&(objectClass=java.lang.Boolean)(bundle.id=" + sc1.getBundleId() + "))"), null);

		cp2Tracker.open();
		sc1Tracker.open();

		assertNotNull("The cp2 bundle never found the service.", cp2Tracker.waitForService(2000));
		assertNotNull("The sc1 bundle never found the service.", sc1Tracker.waitForService(2000));
		cp2Tracker.close();
		sc1Tracker.close();
	}

	public void testSingleBundleRegions() throws BundleException, InvalidSyntaxException, InterruptedException {
		// get the system region
		Region systemRegion = digraph.getRegion(0);
		Map<String, Bundle> bundles = new HashMap<String, Bundle>();
		// create a disconnected test region for each test bundle
		for (String location : ALL) {
			Region testRegion = digraph.createRegion(location);
			bundles.put(location, testRegion.installBundle(bundleInstaller.getBundleLocation(location)));
			// Import the system bundle from the systemRegion
			digraph.connect(testRegion, digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, "(id=0)").build(), systemRegion);
			// must import Boolean services into systemRegion to test
			digraph.connect(systemRegion, digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, "(objectClass=java.lang.Boolean)").build(), testRegion);
		}

		bundleInstaller.resolveBundles(bundles.values().toArray(new Bundle[bundles.size()]));
		assertEquals(PP1, Bundle.RESOLVED, bundles.get(PP1).getState());
		assertEquals(SP1, Bundle.INSTALLED, bundles.get(SP1).getState());
		assertEquals(CP1, Bundle.RESOLVED, bundles.get(CP1).getState());
		assertEquals(PP2, Bundle.INSTALLED, bundles.get(PP2).getState());
		assertEquals(SP2, Bundle.INSTALLED, bundles.get(SP2).getState());
		assertEquals(CP2, Bundle.INSTALLED, bundles.get(CP2).getState());
		assertEquals(BC1, Bundle.INSTALLED, bundles.get(BC1).getState());
		assertEquals(SC1, Bundle.INSTALLED, bundles.get(SC1).getState());
		assertEquals(CC1, Bundle.INSTALLED, bundles.get(CC1).getState());

		// now make the necessary connections
		// SP1
		digraph.connect(
				digraph.getRegion(SP1), 
				digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE, "(" + RegionFilter.VISIBLE_PACKAGE_NAMESPACE + "=pkg1.*)").build(),
				digraph.getRegion(PP1));
		// PP2
		digraph.connect(
				digraph.getRegion(PP2), 
				digraph.createRegionFilterBuilder().allow(CP1, "(name=" + CP1 + ")").build(),
				digraph.getRegion(CP1));
		// SP2
		digraph.connect(
				digraph.getRegion(SP2), 
				digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE, "(" + RegionFilter.VISIBLE_PACKAGE_NAMESPACE + "=pkg2.*)").build(),
				digraph.getRegion(PP2));
		// CP2
		digraph.connect(
				digraph.getRegion(CP2), 
				digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE, "(" + RegionFilter.VISIBLE_PACKAGE_NAMESPACE + "=pkg1.*)").build(),
				digraph.getRegion(PP1));
		digraph.connect(
				digraph.getRegion(CP2), 
				digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, "(" + Constants.OBJECTCLASS + "=pkg1.*)").build(),
				digraph.getRegion(SP1));
		// PC1
		digraph.connect(
				digraph.getRegion(PC1), 
				digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE, "(" + RegionFilter.VISIBLE_PACKAGE_NAMESPACE + "=pkg2.*)").build(),
				digraph.getRegion(PP2));
		// BC1
		digraph.connect(
				digraph.getRegion(BC1), 
				digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_REQUIRE_NAMESPACE, "(" + RegionFilter.VISIBLE_REQUIRE_NAMESPACE + "=" + PP2 + ")").build(),
				digraph.getRegion(PP2));
		// SC1
		digraph.connect(
				digraph.getRegion(SC1), 
				digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE, "(" + RegionFilter.VISIBLE_PACKAGE_NAMESPACE + "=pkg2.*)").build(),
				digraph.getRegion(PP2));
		digraph.connect(
				digraph.getRegion(SC1), 
				digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, "(" + Constants.OBJECTCLASS + "=pkg2.*)").build(),
				digraph.getRegion(SP2));
		// CC1
		digraph.connect(
				digraph.getRegion(CC1), 
				digraph.createRegionFilterBuilder().allow(CP2, "(name=" + CP2 + ")").build(),
				digraph.getRegion(CP2));

		bundleInstaller.resolveBundles(bundles.values().toArray(new Bundle[bundles.size()]));
		for (Bundle bundle : bundles.values()) {
			assertEquals("Bundle did not resolve: " + bundle.getSymbolicName(), Bundle.RESOLVED, bundle.getState());
			bundle.start();
		}
		ServiceTracker<Boolean, Boolean> cp2Tracker = new ServiceTracker<Boolean, Boolean>(context, context.createFilter("(&(objectClass=java.lang.Boolean)(bundle.id=" + bundles.get(CP2).getBundleId() + "))"), null);
		ServiceTracker<Boolean, Boolean> sc1Tracker = new ServiceTracker<Boolean, Boolean>(context, context.createFilter("(&(objectClass=java.lang.Boolean)(bundle.id=" + bundles.get(SC1).getBundleId() + "))"), null);

		cp2Tracker.open();
		sc1Tracker.open();

		assertNotNull("The cp2 bundle never found the service.", cp2Tracker.waitForService(2000));
		assertNotNull("The sc1 bundle never found the service.", sc1Tracker.waitForService(2000));
		cp2Tracker.close();
		sc1Tracker.close();
	}

	public void testCyclicRegions0() throws BundleException, InvalidSyntaxException, InterruptedException {
		doCyclicRegions(0);
	}

	public void testCyclicRegions10() throws BundleException, InvalidSyntaxException, InterruptedException {
		doCyclicRegions(10);
	}
	public void testCyclicRegions100() throws BundleException, InvalidSyntaxException, InterruptedException {
		doCyclicRegions(100);
	}

	private void doCyclicRegions(int numLevels) throws BundleException, InvalidSyntaxException, InterruptedException {
		String regionName1 = getName() + "_1";
		String regionName2 = getName() + "_2";
		// get the system region
		Region systemRegion = digraph.getRegion(0);
		// create two regions to hold the bundles
		Region testRegion1 = digraph.createRegion(regionName1);
		Region testRegion2 = digraph.createRegion(regionName2);
		// connect to the system bundle
		testRegion1.connectRegion(systemRegion, digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, "(id=0)").build());
		testRegion2.connectRegion(systemRegion, digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, "(id=0)").build());
		// must import Boolean services into systemRegion to test
		systemRegion.connectRegion(testRegion1, digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, "(objectClass=java.lang.Boolean)").build());
		systemRegion.connectRegion(testRegion2, digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, "(objectClass=java.lang.Boolean)").build());

		Map<String, Bundle> bundles = new HashMap<String, Bundle>();
		// add bundles to region1
		bundles.put(PP1, testRegion1.installBundle(bundleInstaller.getBundleLocation(PP1)));
		bundles.put(SP2, testRegion1.installBundle(bundleInstaller.getBundleLocation(SP2)));
		bundles.put(CP2, testRegion1.installBundle(bundleInstaller.getBundleLocation(CP2)));
		bundles.put(PC1, testRegion1.installBundle(bundleInstaller.getBundleLocation(PC1)));
		bundles.put(BC1, testRegion1.installBundle(bundleInstaller.getBundleLocation(BC1)));

		// add bundles to region2
		bundles.put(SP1, testRegion2.installBundle(bundleInstaller.getBundleLocation(SP1)));
		bundles.put(CP1, testRegion2.installBundle(bundleInstaller.getBundleLocation(CP1)));
		bundles.put(PP2, testRegion2.installBundle(bundleInstaller.getBundleLocation(PP2)));
		bundles.put(SC1, testRegion2.installBundle(bundleInstaller.getBundleLocation(SC1)));
		bundles.put(CC1, testRegion2.installBundle(bundleInstaller.getBundleLocation(CC1)));

		RegionFilterBuilder testRegionFilter1 = digraph.createRegionFilterBuilder();
		// SP2 -> PP2
		testRegionFilter1.allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE, "(" + RegionFilter.VISIBLE_PACKAGE_NAMESPACE + "=pkg2.*)");
		// CP2 -> SP1
		testRegionFilter1.allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, "(" + Constants.OBJECTCLASS + "=pkg1.*)");
		// PC1 -> PP2
		//testRegionFilter1.allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE, "(" + RegionFilter.VISIBLE_PACKAGE_NAMESPACE + "=pkg2.*)");
		// BC1 -> PP2
		testRegionFilter1.allow(RegionFilter.VISIBLE_REQUIRE_NAMESPACE, "(" + RegionFilter.VISIBLE_REQUIRE_NAMESPACE + "=" + PP2 + ")");

		RegionFilterBuilder testRegionFilter2 = digraph.createRegionFilterBuilder();
		//SP1 -> PP1
		testRegionFilter2.allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE, "(" + RegionFilter.VISIBLE_PACKAGE_NAMESPACE + "=pkg1.*)");
		//SC1 -> SP2
		testRegionFilter2.allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, "(" + Constants.OBJECTCLASS + "=pkg2.*)");
		//CC1 -> CP2
		testRegionFilter2.allow(CP2, "(name=" + CP2 + ")");

		Region r1, r2 = null;
		for (int i = 0; i <=numLevels; i++) {
			r1 = (i > 0) ? r2 : testRegion1;
			r2 = (i < numLevels) ? digraph.createRegion(getName() + "_level_" + i) : testRegion2;
			r1.connectRegion(r2, testRegionFilter1.build());
			r2.connectRegion(r1, testRegionFilter2.build());
		}

		bundleInstaller.resolveBundles(bundles.values().toArray(new Bundle[bundles.size()]));
		for (Bundle bundle : bundles.values()) {
			assertEquals("Bundle did not resolve: " + bundle.getSymbolicName(), Bundle.RESOLVED, bundle.getState());
			bundle.start();
		}
		ServiceTracker<Boolean, Boolean> cp2Tracker = new ServiceTracker<Boolean, Boolean>(context, context.createFilter("(&(objectClass=java.lang.Boolean)(bundle.id=" + bundles.get(CP2).getBundleId() + "))"), null);
		ServiceTracker<Boolean, Boolean> sc1Tracker = new ServiceTracker<Boolean, Boolean>(context, context.createFilter("(&(objectClass=java.lang.Boolean)(bundle.id=" + bundles.get(SC1).getBundleId() + "))"), null);

		cp2Tracker.open();
		sc1Tracker.open();

		assertNotNull("The cp2 bundle never found the service.", cp2Tracker.waitForService(2000));
		assertNotNull("The sc1 bundle never found the service.", sc1Tracker.waitForService(2000));
		cp2Tracker.close();
		sc1Tracker.close();
	}
}
