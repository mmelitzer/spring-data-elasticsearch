/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.repository.support;

import static org.apache.commons.lang.RandomStringUtils.*;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.elasticsearch.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.elasticsearch.entities.SampleEntity;
import org.springframework.data.elasticsearch.repositories.sample.SampleElasticsearchRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/simple-repository-test.xml")
public class SimpleElasticsearchRepositoryTests {

	@Autowired
	private SampleElasticsearchRepository repository;

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;


	@Before
	public void before() {
		elasticsearchTemplate.deleteIndex(SampleEntity.class);
		elasticsearchTemplate.createIndex(SampleEntity.class);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
	}

	@Test
	public void shouldDoBulkIndexDocument() {
		// given
		String documentId1 = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId1);
		sampleEntity1.setMessage("some message");
		sampleEntity1.setVersion(System.currentTimeMillis());

		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("some message");
		sampleEntity2.setVersion(System.currentTimeMillis());

		// when
		repository.save(Arrays.asList(sampleEntity1, sampleEntity2));
		// then
		SampleEntity entity1FromElasticSearch = repository.findOne(documentId1);
		assertThat(entity1FromElasticSearch, is(notNullValue()));

		SampleEntity entity2FromElasticSearch = repository.findOne(documentId2);
		assertThat(entity2FromElasticSearch, is(notNullValue()));
	}

	@Test
	public void shouldSaveDocument() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());
		// when
		repository.save(sampleEntity);
		// then
		SampleEntity entityFromElasticSearch = repository.findOne(documentId);
		assertThat(entityFromElasticSearch, is(notNullValue()));
	}

	@Test
	public void shouldSaveDocumentWithoutId() {
		// given
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());
		// when
		repository.save(sampleEntity);
		// then
		assertThat(sampleEntity.getId(), is(notNullValue()));
	}

	@Test
	public void shouldFindDocumentById() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);
		// when
		SampleEntity entityFromElasticSearch = repository.findOne(documentId);
		// then
		assertThat(entityFromElasticSearch, is(notNullValue()));
		assertThat(sampleEntity, is((equalTo(sampleEntity))));
	}

	@Test
	public void shouldReturnCountOfDocuments() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);
		// when
		Long count = repository.count();
		// then
		assertThat(count, is(greaterThanOrEqualTo(1L)));
	}

	@Test
	public void shouldFindAllDocuments() {
		// when
		Iterable<SampleEntity> results = repository.findAll();
		// then
		assertThat(results, is(notNullValue()));
	}

	@Test
	public void shouldDeleteDocument() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);
		// when
		repository.delete(documentId);
		// then
		SampleEntity entityFromElasticSearch = repository.findOne(documentId);
		assertThat(entityFromElasticSearch, is(nullValue()));
	}

	@Test
	public void shouldSearchDocumentsGivenSearchQuery() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some test message");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);

		SearchQuery query = new NativeSearchQueryBuilder().withQuery(termQuery("message", "test")).build();
		// when
		Page<SampleEntity> page = repository.search(query);
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getNumberOfElements(), is(greaterThanOrEqualTo(1)));
	}

	@Test
	public void shouldSearchDocumentsGivenElasticsearchQuery() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("hello world.");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);
		// when
		Page<SampleEntity> page = repository.search(termQuery("message", "world"), new PageRequest(0, 50));
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getNumberOfElements(), is(greaterThanOrEqualTo(1)));
	}

	/*
	DATAES-82
	*/
	@Test
	public void shouldFindAllByIdQuery() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("hello world.");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);

		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("hello world.");
		sampleEntity2.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity2);

		// when
		Iterable<SampleEntity> sampleEntities = repository.findAll(Arrays.asList(documentId, documentId2));

		// then
		assertNotNull("sample entities cant be null..", sampleEntities);
		List<SampleEntity> entities = Lists.newArrayList(sampleEntities);
		assertThat(entities.size(), is(2));
	}

	@Test
	public void shouldSaveIterableEntities() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setMessage("hello world.");
		sampleEntity1.setVersion(System.currentTimeMillis());

		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("hello world.");
		sampleEntity2.setVersion(System.currentTimeMillis());

		Iterable<SampleEntity> sampleEntities = Arrays.asList(sampleEntity1, sampleEntity2);
		// when
		repository.save(sampleEntities);
		// then
		Page<SampleEntity> entities = repository.search(termQuery("id", documentId), new PageRequest(0, 50));
		assertNotNull(entities);
	}

	@Test
	public void shouldReturnTrueGivenDocumentWithIdExists() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("hello world.");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);

		// when
		boolean exist = repository.exists(documentId);

		// then
		assertEquals(exist, true);
	}

	@Test
	public void shouldReturnResultsForGivenSearchQuery() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("hello world.");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);
		// when
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(termQuery("id", documentId)).build();
		Page<SampleEntity> sampleEntities = repository.search(searchQuery);
		// then
		assertThat(sampleEntities.getTotalElements(), equalTo(1L));
	}

	@Test
	public void shouldDeleteAll() {
		// when
		repository.deleteAll();
		// then
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		Page<SampleEntity> sampleEntities = repository.search(searchQuery);
		assertThat(sampleEntities.getTotalElements(), equalTo(0L));
	}

	@Test
	public void shouldDeleteEntity() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("hello world.");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);
		// when
		repository.delete(sampleEntity);
		// then
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(termQuery("id", documentId)).build();
		Page<SampleEntity> sampleEntities = repository.search(searchQuery);
		assertThat(sampleEntities.getTotalElements(), equalTo(0L));
	}

	@Test
	public void shouldReturnIterableEntities() {
		// given
		String documentId1 = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId1);
		sampleEntity1.setMessage("hello world.");
		sampleEntity1.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity1);

		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("hello world.");
		sampleEntity2.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity2);

		// when
		Iterable<SampleEntity> sampleEntities = repository.search(termQuery("id", documentId1));
		// then
		assertNotNull("sample entities cant be null..", sampleEntities);
	}

	@Test
	public void shouldDeleteIterableEntities() {
		// given
		String documentId1 = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId1);
		sampleEntity1.setMessage("hello world.");
		sampleEntity1.setVersion(System.currentTimeMillis());

		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("hello world.");
		sampleEntity2.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity2);

		Iterable<SampleEntity> sampleEntities = Arrays.asList(sampleEntity2, sampleEntity2);
		// when
		repository.delete(sampleEntities);
		// then
		assertThat(repository.findOne(documentId1), is(nullValue()));
		assertThat(repository.findOne(documentId2), is(nullValue()));
	}

	@Test
	public void shouldIndexEntity() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setVersion(System.currentTimeMillis());
		sampleEntity.setMessage("some message");
		// when
		repository.index(sampleEntity);
		// then
		Page<SampleEntity> entities = repository.search(termQuery("id", documentId), new PageRequest(0, 50));
		assertThat(entities.getTotalElements(), equalTo(1L));
	}

	@Test
	public void shouldSortByGivenField() {
		// todo
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("world");
		repository.save(sampleEntity);

		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("hello");
		repository.save(sampleEntity2);
		// when
		Iterable<SampleEntity> sampleEntities = repository.findAll(new Sort(new Sort.Order(Sort.Direction.ASC, "message")));
		// then
		assertThat(sampleEntities, is(notNullValue()));
	}

	@Test
	public void shouldReturnSimilarEntities() {
		// given
		String sampleMessage = "So we build a web site or an application and want to add search to it, "
				+ "and then it hits us: getting search working is hard. We want our search solution to be fast,"
				+ " we want a painless setup and a completely free search schema, we want to be able to index data simply using JSON over HTTP, "
				+ "we want our search server to be always available, we want to be able to start with one machine and scale to hundreds, "
				+ "we want real-time search, we want simple multi-tenancy, and we want a solution that is built for the cloud.";

		List<SampleEntity> sampleEntities = createSampleEntitiesWithMessage(sampleMessage, 30);
		repository.save(sampleEntities);

		// when
		Page<SampleEntity> results = repository.searchSimilar(sampleEntities.get(0), new String[]{"message"}, new PageRequest(0, 5));

		// then
		assertThat(results.getTotalElements(), is(greaterThanOrEqualTo(1L)));
	}

	private static List<SampleEntity> createSampleEntitiesWithMessage(String message, int numberOfEntities) {
		List<SampleEntity> sampleEntities = new ArrayList<SampleEntity>();
		for (int i = 0; i < numberOfEntities; i++) {
			String documentId = randomNumeric(5);
			SampleEntity sampleEntity = new SampleEntity();
			sampleEntity.setId(documentId);
			sampleEntity.setMessage(message);
			sampleEntity.setRate(2);
			sampleEntity.setVersion(System.currentTimeMillis());
			sampleEntities.add(sampleEntity);
		}
		return sampleEntities;
	}
}
