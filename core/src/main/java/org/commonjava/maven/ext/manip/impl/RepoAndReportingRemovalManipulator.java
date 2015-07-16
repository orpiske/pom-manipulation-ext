/**
 * Copyright (C) 2012 Red Hat, Inc. (jcasey@redhat.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.maven.ext.manip.impl;

import static org.commonjava.maven.ext.manip.util.IdUtils.ga;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.codehaus.plexus.component.annotations.Component;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.state.ManipulationSession;
import org.commonjava.maven.ext.manip.state.RepoReportingState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Manipulator} implementation that can remove Reporting and Repository sections from a project's pom file.
 * Configuration is stored in a {@link RepoReportingState} instance, which is in turn stored in the {@link ManipulationSession}.
 */
@Component( role = Manipulator.class, hint = "enforce-repo-reporting-removal" )
public class RepoAndReportingRemovalManipulator
    implements Manipulator
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    /**
     * No prescanning required for Repository and Reporting Removal.
     */
    @Override
    public void scan( final List<Project> projects, final ManipulationSession session )
        throws ManipulationException
    {
    }

    /**
     * Initialize the {@link RepoReportingState} state holder in the {@link ManipulationSession}. This state holder detects
     * version-change configuration from the Maven user properties (-D properties from the CLI) and makes it available for
     * later invocations of {@link RepoAndReportingRemovalManipulator#scan(List, ManipulationSession)} and the apply* methods.
     */
    @Override
    public void init( final ManipulationSession session )
    {
        final Properties userProps = session.getUserProperties();
        session.setState( new RepoReportingState( userProps ) );
    }

    /**
     * Apply the reporting and repository removal changes to the list of {@link Project}'s given.
     */
    @Override
    public Set<Project> applyChanges( final List<Project> projects, final ManipulationSession session )
        throws ManipulationException
    {
        final RepoReportingState state = session.getState( RepoReportingState.class );
        if ( !session.isEnabled() || !state.isEnabled() )
        {
            logger.debug( getClass().getSimpleName() + ": Nothing to do!" );
            return Collections.emptySet();
        }

        final Set<Project> changed = new HashSet<Project>();

        for ( final Project project : projects )
        {
            final String ga = ga( project );
            logger.info( getClass().getSimpleName() + " applying changes to: " + ga );
            final Model model = project.getModel();

            if ( model.getRepositories() != null && !model.getRepositories()
                                                          .isEmpty() )
            {
                model.setRepositories( Collections.<Repository> emptyList() );
                changed.add( project );
            }

            if ( model.getPluginRepositories() != null && !model.getPluginRepositories()
                                                                .isEmpty() )
            {
                model.setPluginRepositories( Collections.<Repository> emptyList() );
                changed.add( project );
            }

            if ( model.getReporting() != null )
            {
                model.setReporting( null );
                changed.add( project );
            }

            // remove repositories in the profiles as well
            final List<Profile> profiles = model.getProfiles();

            if ( !profiles.isEmpty() )
            {
                for ( final Profile profile : profiles )
                {
                    if ( !profile.getRepositories().isEmpty() )
                    {
                        profile.setRepositories( Collections.<Repository> emptyList() );
                        changed.add( project );
                    }

                    if ( !profile.getPluginRepositories().isEmpty() )
                    {
                        profile.setPluginRepositories( Collections.<Repository> emptyList() );
                        changed.add( project );
                    }

                    if ( profile.getReporting() != null )
                    {
                        profile.setReporting( null );
                        changed.add( project );
                    }
                }

            }

        }

        return changed;
    }

    @Override
    public int getExecutionIndex()
    {
        return 45;
    }
}
