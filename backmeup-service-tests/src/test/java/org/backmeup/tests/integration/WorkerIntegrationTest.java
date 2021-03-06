package org.backmeup.tests.integration;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;

import org.backmeup.model.dto.WorkerInfoDTO;
import org.backmeup.model.dto.WorkerMetricDTO;
import org.backmeup.model.dto.WorkerConfigDTO.DistributionMechanism;
import org.backmeup.tests.IntegrationTest;
import org.backmeup.tests.integration.utils.BackMeUpUtils;
import org.backmeup.tests.integration.utils.TestDataManager;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.jayway.restassured.internal.mapper.ObjectMapperType;

@Category(IntegrationTest.class)
public class WorkerIntegrationTest extends IntegrationTestBase {
    
    @Test
    public void testInitializeWorker() {
        String accessToken = BackMeUpUtils.authenticateWorker(TestDataManager.WORKER_ID, TestDataManager.WORKER_SECRET);
        WorkerInfoDTO workerInfo = TestDataManager.getWorkerInfo();

        try {
            given()
                .log().all()
                .header("Accept", "application/json")
                .header("Authorization", accessToken)
                .body(workerInfo, ObjectMapperType.JACKSON_1)
            .when()
                .put("/workers/hello")
            .then()
                .log().all()
                .statusCode(200)
                .body("distributionMechanism", equalTo(DistributionMechanism.QUEUE.toString()))
                .body(containsString("connectionInfo"))
                .body(containsString("backupNameTemplate"))
                .body(containsString("pluginsExportedPackages"));
        } finally {
        }
    }
    
    @Test
    public void testAddWorkerMetrics() {
        String accessToken = BackMeUpUtils.authenticateWorker(TestDataManager.WORKER_ID, TestDataManager.WORKER_SECRET);
        List<WorkerMetricDTO> metrics = TestDataManager.getWorkerMetrics();
        
        try {
            given()
                .log().all()
                .header("Accept", "application/json")
                .header("Authorization", accessToken)
                .body(metrics, ObjectMapperType.JACKSON_1)
            .when()
                .post("/workers/metrics")
            .then()
                .log().all()
                .statusCode(204);
        } finally {
        }
    }
}
