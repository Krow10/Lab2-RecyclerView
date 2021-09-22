# [GTI785 Systèmes d'applications mobiles]<br/>Lab 2 : RecyclerView, Contacts App
## Présentation
L'objectif pour ce 2<sup>ème</sup> laboratoire est d'apprendre à utiliser le composant ```RecyclerView``` et de comprendre son mode de fontionnement (```Adapter```, ```ViewHolder```, gestion des données, ...) à travers une application d'affichage de contacts (fictifs).

L'interface se compose donc d'une liste de contact (```CardView```) que l'on peut modifier en cliquant ou en glissant la carte du contact que l'on souhaite supprimer. Il est ensuite possible de restaurer le contact via un ```FloatingActionButton``` activé lorsque la corbeille est remplie.

Voici une courte vidéo de l'application en action :

https://user-images.githubusercontent.com/23462475/134288239-b31183d0-a9c9-41bd-8225-9aae75963e0e.mp4

Quelques fonctionnalités supplémentaires ont été rajoutées :
- Animation des cartes de contacts lors de l'ajout, du mouvement, ou de la suppression de la carte.
- Ajout d'une icône de corbeille dynamique pour afficher le nombre d'éléments supprimés.
- Ajout d'une icône de rafraichissement pour réinitialiser la liste de contacts.
- Bouton flottant de restauration de contact dynamique (désactivé lorsque la corbeille est vide). 

## Installation

Récupérer la dernière version de l'apk depuis la [page de publication](https://github.com/Krow10/Lab2-RecyclerView/releases/) ou compilez là vous même à l'aide de Gradle !

## License

Distribué sous la license MIT. Voir le fichier [LICENSE](https://github.com/Krow10/Lab2-RecyclerView/blob/master/LICENSE) pour plus d'informations.

## Contact

Etienne Donneger - etienne.donneger.1@ens.etsmtl.ca

## Remerciements

[randomuser.me](https://randomuser.me/api) - A free, open-source API for generating random user data. Like Lorem Ipsum, but for people.
