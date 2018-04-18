package org.ostelco.topup.api.db;

import org.ostelco.topup.api.core.Profile;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.testing.LocalDatastoreHelper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;

/**
 *
 */
public class SubscriberDAOTest {

    private static Datastore store;

    private SubscriberDAO dao;

    @ClassRule
    public static void setupDatastore() throws IOException, InterruptedException {
        LocalDatastoreHelper helper = LocalDatastoreHelper.create(1.0);
        helper.start();
        store = helper.getOptions().getService();
    }

    @Before
    public void setUp() throws Exception {
        dao = new SubscriberDAOImpl(store);
    }

}
