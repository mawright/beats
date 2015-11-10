/**
 * Copyright (c) 2012, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *   Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *   Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 **/

package edu.berkeley.path.beats;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import edu.berkeley.path.beats.util.SchemaUtil;
import org.apache.log4j.Logger;

import edu.berkeley.path.beats.simulator.utils.BeatsException;

/**
 * Retrieves and stores application schema and engine versions
 */
@SuppressWarnings("restriction")
public class Version {

	private static Logger logger = Logger.getLogger(Version.class);

	public static void main(String[] args) {
		System.out.println(Version.print());
	}

    public static String getGitHash(){
        InputStream inputStream = Runner.class.getResourceAsStream("/buildNumber.properties");
        Properties properties = new Properties();
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read properties file", e);
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
        return properties.getProperty("git-sha-1");
    }

	public static String getSchemaVersion() {
		String x = "";
		try {
			x = SchemaUtil.getSchemaVersion();
		} catch (BeatsException exc) {
			logger.error("Failed to retrieve schema version", exc);
		}
		return x;
	}

	public static String getEngineVersion() {
		String x = "";
		java.io.InputStream istream = Version.class.getClassLoader().getResourceAsStream("engine.version");
		if (null != istream) {
			BufferedReader br = new BufferedReader(new InputStreamReader(istream));
			try{
				x = br.readLine();
				br.close();
			} catch (IOException exc) {
				logger.error("Failed to retrieve engine version", exc);
			}
		}
		return x;
	}

	public static String getJavaVersion() {
		return System.getProperty("java.version");
	}

//	public static Version get() {
//		Version version = new Version();
//
//		String schema = Version.getSchemaVersion();
//		String engine = Version.getEngineVersion();
//
//		return version;
//	}

	public static String print() {
		final String linesep = System.getProperty("line.separator");
		StringBuilder sb = new StringBuilder();
        sb.append("git: ").append(getGitHash());
        sb.append(linesep);
		sb.append("schema: ").append(getSchemaVersion());
		sb.append(linesep);
		sb.append("engine: ").append(getEngineVersion());
		sb.append(linesep);
		sb.append("java:   ").append(getJavaVersion());
		return sb.toString();
	}
}
