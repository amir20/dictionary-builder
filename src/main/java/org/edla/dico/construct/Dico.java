package org.edla.dico.construct;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.edla.tools.Locator;
import org.edla.wikimediaschema.MediaWikiType;
import org.edla.wikimediaschema.PageType;
import org.edla.wikimediaschema.RevisionType;

/**
 * @author hack
 * 
 */

public class Dico {

	private MediaWikiType mediaWikiType;

	private Locator locator;

	public Dico(final String xmlFile) {
		this.locator = Locator.getInstance();
		this.mediaWikiType = this.getMediaWikiType(xmlFile);
	}

	private MediaWikiType getMediaWikiType(final String xmlFile) {
		try {
			final JAXBContext jc = JAXBContext.newInstance(
					"org.edla.wikimediaschema", this.getClass()
							.getClassLoader());
			final Unmarshaller unmarshaller = jc.createUnmarshaller();
			final Object j = unmarshaller.unmarshal(new File(xmlFile));
			final JAXBElement e = (JAXBElement) j;
			return (MediaWikiType) e.getValue();
		} catch (final JAXBException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void buildWordList(final String wordsFile, final String exclusFile) {
		int exclus = 0;
		String word;
		String definition;
		final List<PageType> pages = this.mediaWikiType.getPage();
		try {

			final FileWriter wordsWriter = new FileWriter(wordsFile);
			final FileWriter exclusWriter = new FileWriter(exclusFile);
			for (int i = 0; i < pages.size(); i++) {
				final PageType p = pages.get(i);
				word = p.getTitle();
				definition = this.filtre(p,word);
				if (definition != null) {
					wordsWriter.write(word);
					wordsWriter.write('\n');
					this.buildDefinitionFiles(word,definition);
				} else {
					exclusWriter.write(word);
					exclusWriter.write('\n');
					exclus ++;
				}
			}

			wordsWriter.close();
			exclusWriter.close();
			System.out.println("entrées:" + pages.size());
			System.out.println("nb exclus:" + exclus);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private String filtre(final PageType p, final String word) {
		String definition = null;
		if (word.contains("/") || word.contains(":")) {
			return null;
		}
		final List revisions = p.getRevisionOrUpload();
		for (int j = 0; j < revisions.size(); j++) {
			final RevisionType r = (RevisionType) revisions.get(j);
			definition = r.getText().getValue();
		}
		if (!definition.contains("{{=fr=}}")) {
			return null;
		}
		return definition;
	}

	private void buildDefinitionFiles(final String word, final String definition) {
		
		final String directory = this.locator.locate(word);

		try {
			new File(directory).mkdirs();
			final BufferedOutputStream writer = new BufferedOutputStream(
					new GZIPOutputStream(new FileOutputStream(directory + "/"
							+ word + ".gz")));
			/*
			 * ZipOutputStream writer = new ZipOutputStream( new
			 * BufferedOutputStream(new FileOutputStream(directory + "/" + word +
			 * ".zip")));
			 */
			writer.write(definition.getBytes());

			// FileWriter writer = new FileWriter(directory+"/" + word);
			// writer.write(definition);
			writer.close();

		} catch (final IOException e) {
			System.out.println(word + " " + directory);
			e.printStackTrace();
		}
	}
	

}