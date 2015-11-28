/*
 * Copyright (c) 2015
 * SwarmBox
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of the {organization} nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.swarmbox.netsuite;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.netsuite.webservices.platform.core_2015_2.Record;
import com.netsuite.webservices.platform.core_2015_2.SearchRecordBasic;
import com.netsuite.webservices.platform.core_2015_2.SearchResult;
import com.netsuite.webservices.platform_2015_2.NetSuitePortType;
import com.swarmbox.mongodb.ArrayCodecProvider;
import com.swarmbox.mongodb.BeanCodecProvider;
import com.swarmbox.mongodb.GregorianCalendarCodec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static com.swarmbox.netsuite.Helpers.*;

public class Export {

    private static final Logger LOG = LoggerFactory.getLogger(Export.class);

    private static final String[] TYPES = {
            "com.netsuite.webservices.platform.common_2015_2.AccountingPeriodSearchBasic",
            //"com.netsuite.webservices.platform.common_2015_2.AccountingTransactionSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.AccountSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.AddressSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.AppDefinitionSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.AppPackageSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.BillingAccountSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.BillingScheduleSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.BinSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.BudgetSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.CalendarEventSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.CampaignSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.ChargeSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.ClassificationSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.ContactCategorySearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.ContactRoleSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.ContactSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.CouponCodeSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.CurrencyRateSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.CustomerCategorySearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.CustomerMessageSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.CustomerSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.CustomerStatusSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.CustomListSearchBasic",
            //"com.netsuite.webservices.platform.common_2015_2.CustomRecordSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.DepartmentSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.EmployeeSearchBasic",
            //"com.netsuite.webservices.platform.common_2015_2.EntityGroupSearchBasic",
            //"com.netsuite.webservices.platform.common_2015_2.EntitySearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.ExpenseCategorySearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.FileSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.FolderSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.GiftCertificateSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.GlobalAccountMappingSearchBasic",
            //"com.netsuite.webservices.platform.common_2015_2.GroupMemberSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.InventoryDetailSearchBasic",
            //"com.netsuite.webservices.platform.common_2015_2.InventoryNumberBinSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.InventoryNumberSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.IssueSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.ItemAccountMappingSearchBasic",
            //"com.netsuite.webservices.platform.common_2015_2.ItemBinNumberSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.ItemDemandPlanSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.ItemRevisionSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.ItemSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.ItemSupplyPlanSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.JobSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.JobStatusSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.JobTypeSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.LocationSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.ManufacturingCostTemplateSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.ManufacturingOperationTaskSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.ManufacturingRoutingSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.MessageSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.NexusSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.NoteSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.NoteTypeSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.OpportunitySearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.OriginatingLeadSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.OtherNameCategorySearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.PartnerCategorySearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.PartnerSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.PaymentMethodSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.PayrollItemSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.PhoneCallSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.PriceLevelSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.PricingGroupSearchBasic",
            //"com.netsuite.webservices.platform.common_2015_2.PricingSearchBasic",
            //"com.netsuite.webservices.platform.common_2015_2.ProjectTaskAssignmentSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.ProjectTaskSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.PromotionCodeSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.ResourceAllocationSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.RevRecScheduleSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.RevRecTemplateSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.SalesRoleSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.SiteCategorySearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.SolutionSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.SubsidiarySearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.SupportCaseSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.TaskSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.TermSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.TimeBillSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.TimeEntrySearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.TimeSheetSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.TopicSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.TransactionSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.UnitsTypeSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.VendorCategorySearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.VendorSearchBasic",
            "com.netsuite.webservices.platform.common_2015_2.WinLossReasonSearchBasic"
    };

    private static String f(long nanoSeconds) {
        return String.format("%02dh %02dm %02ds",
                TimeUnit.NANOSECONDS.toHours(nanoSeconds),
                TimeUnit.NANOSECONDS.toMinutes(nanoSeconds) - TimeUnit.HOURS.toMinutes(TimeUnit.NANOSECONDS.toHours(nanoSeconds)),
                TimeUnit.NANOSECONDS.toSeconds(nanoSeconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.NANOSECONDS.toMinutes(nanoSeconds))
        );
    }

    public static void main(String[] args) {
        MongoClient client = null;

        try {
            /* NetSuite */

            final NetSuitePortType stub = nsBuildAuthenticatedStub();

            /* MongoDB */

            final ServerAddress serverAddress = new ServerAddress(getConfiguration().getString("mg-server"));
            final String databaseName = getConfiguration().getString("mg-database");
            final CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
                    CodecRegistries.fromCodecs(new GregorianCalendarCodec()),
                    CodecRegistries.fromProviders(new ArrayCodecProvider(), new BeanCodecProvider()),
                    MongoClient.getDefaultCodecRegistry()
            );

            client = new MongoClient(serverAddress, MongoClientOptions.builder().codecRegistry(codecRegistry).build());

            //client.dropDatabase(databaseName);
            final MongoDatabase database = client.getDatabase(databaseName);

            /* Dump All Data */

            long as = System.nanoTime();
            long te = as;

            for (String type : TYPES) {
                final String collectionName = type.substring(type.lastIndexOf('.') + 1, type.length() - 11);
                final MongoCollection<Record> collection = database.getCollection(collectionName, Record.class);
                final SearchRecordBasic search = Class.forName(type).asSubclass(SearchRecordBasic.class).newInstance();
                int recordsProcessed = 0;
                long ts = System.nanoTime();
                long ps = ts;

                SearchResult result = nsSearch(stub, search, 5, null);
                final int totalRecords = result.getTotalRecords() != null ? result.getTotalRecords() : 0;

                if (totalRecords > 0) {
                    int pageIndex = result.getPageIndex();
                    final int totalPages = result.getTotalPages();
                    while (pageIndex <= totalPages) {
                        Record[] records = result.getRecordList().getRecord();
                        collection.insertMany(new ArrayList<>(Arrays.asList(records)));

                        te = System.nanoTime();
                        LOG.debug("Exported {} of {} records into {} (Page {} of {} | {} | {})",
                                recordsProcessed += records.length, totalRecords,
                                collectionName,
                                pageIndex, totalPages,
                                f(te - ps), f(te - as)
                        );

                        if (pageIndex++ < totalPages) {
                            ps = System.nanoTime();
                            result = nsSearchMoreWithId(stub, result.getSearchId(), pageIndex,  5, null);
                        }
                    }
                }
                te = System.nanoTime();
                LOG.debug("Finished exporting {} records for {} in {}", totalRecords, collectionName, f(te - ts));
            }
            LOG.debug("Finished exporting all records in {}", f(te - as));
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }
}
