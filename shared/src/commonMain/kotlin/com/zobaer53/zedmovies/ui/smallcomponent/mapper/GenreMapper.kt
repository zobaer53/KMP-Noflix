
package com.zobaer53.zedmovies.ui.smallcomponent.mapper

import androidx.compose.runtime.Composable
import com.zobaer53.zedmovies.domain.model.GenreModel
import com.zobaer53.zedmovies.data.model.Genre
import com.zobaer53.zedmovies.resources.Res
import com.zobaer53.zedmovies.resources.action
import com.zobaer53.zedmovies.resources.action_adventure
import com.zobaer53.zedmovies.resources.adventure
import com.zobaer53.zedmovies.resources.animation
import com.zobaer53.zedmovies.resources.comedy
import com.zobaer53.zedmovies.resources.crime
import com.zobaer53.zedmovies.resources.documentary
import com.zobaer53.zedmovies.resources.drama
import com.zobaer53.zedmovies.resources.family
import com.zobaer53.zedmovies.resources.fantasy
import com.zobaer53.zedmovies.resources.history
import com.zobaer53.zedmovies.resources.horror
import com.zobaer53.zedmovies.resources.kids
import com.zobaer53.zedmovies.resources.music
import com.zobaer53.zedmovies.resources.mystery
import com.zobaer53.zedmovies.resources.news
import com.zobaer53.zedmovies.resources.reality
import com.zobaer53.zedmovies.resources.romance
import com.zobaer53.zedmovies.resources.science_fiction
import com.zobaer53.zedmovies.resources.science_fiction_fantasy
import com.zobaer53.zedmovies.resources.soap
import com.zobaer53.zedmovies.resources.talk
import com.zobaer53.zedmovies.resources.thriller
import com.zobaer53.zedmovies.resources.tv_movie
import com.zobaer53.zedmovies.resources.war
import com.zobaer53.zedmovies.resources.war_politics
import com.zobaer53.zedmovies.resources.western
import org.jetbrains.compose.resources.stringResource


@Composable
fun List<Genre>.asNames() = map { genre -> stringResource(genre.nameResourceId) }

internal fun List<com.zobaer53.zedmovies.domain.model.GenreModel>.asGenres() = map { genre ->
    Genre(nameResourceId = genre.asNameResourceId())
}

private fun com.zobaer53.zedmovies.domain.model.GenreModel.asNameResourceId() = genresNameResources.getValue(this)

private val genresNameResources = mapOf(
    com.zobaer53.zedmovies.domain.model.GenreModel.ACTION to Res.string.action,
    com.zobaer53.zedmovies.domain.model.GenreModel.ADVENTURE to Res.string.adventure,
    com.zobaer53.zedmovies.domain.model.GenreModel.ACTION_ADVENTURE to Res.string.action_adventure,
    com.zobaer53.zedmovies.domain.model.GenreModel.ANIMATION to Res.string.animation,
    com.zobaer53.zedmovies.domain.model.GenreModel.COMEDY to Res.string.comedy,
    com.zobaer53.zedmovies.domain.model.GenreModel.CRIME to Res.string.crime,
    com.zobaer53.zedmovies.domain.model.GenreModel.DOCUMENTARY to Res.string.documentary,
    com.zobaer53.zedmovies.domain.model.GenreModel.DRAMA to Res.string.drama,
    com.zobaer53.zedmovies.domain.model.GenreModel.FAMILY to Res.string.family,
    com.zobaer53.zedmovies.domain.model.GenreModel.FANTASY to Res.string.fantasy,
    com.zobaer53.zedmovies.domain.model.GenreModel.HISTORY to Res.string.history,
    com.zobaer53.zedmovies.domain.model.GenreModel.HORROR to Res.string.horror,
    com.zobaer53.zedmovies.domain.model.GenreModel.KIDS to Res.string.kids,
    com.zobaer53.zedmovies.domain.model.GenreModel.MUSIC to Res.string.music,
    com.zobaer53.zedmovies.domain.model.GenreModel.MYSTERY to Res.string.mystery,
    com.zobaer53.zedmovies.domain.model.GenreModel.NEWS to Res.string.news,
    com.zobaer53.zedmovies.domain.model.GenreModel.REALITY to Res.string.reality,
    com.zobaer53.zedmovies.domain.model.GenreModel.ROMANCE to Res.string.romance,
    com.zobaer53.zedmovies.domain.model.GenreModel.SCIENCE_FICTION to Res.string.science_fiction,
    com.zobaer53.zedmovies.domain.model.GenreModel.SCIENCE_FICTION_FANTASY to Res.string.science_fiction_fantasy,
    com.zobaer53.zedmovies.domain.model.GenreModel.SOAP to Res.string.soap,
    com.zobaer53.zedmovies.domain.model.GenreModel.TALK to Res.string.talk,
    com.zobaer53.zedmovies.domain.model.GenreModel.THRILLER to Res.string.thriller,
    com.zobaer53.zedmovies.domain.model.GenreModel.TV_MOVIE to Res.string.tv_movie,
    com.zobaer53.zedmovies.domain.model.GenreModel.WAR to Res.string.war,
    com.zobaer53.zedmovies.domain.model.GenreModel.WAR_POLITICS to Res.string.war_politics,
    com.zobaer53.zedmovies.domain.model.GenreModel.WESTERN to Res.string.western
)
